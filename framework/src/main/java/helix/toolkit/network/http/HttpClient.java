package helix.toolkit.network.http;

import helix.exceptions.TooManyHttpRedirects;
import helix.exceptions.UnsupportedHttpVersion;
import helix.toolkit.network.decoders.ChunkedDecoderStream;
import helix.toolkit.network.decoders.IdentityDecoderStream;
import helix.toolkit.streams.StreamUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpClient
{
    /**
     * HTTP version to be used in requests
     * **/
    private static final String VERSION = "1.1";

    /**
     * Maximum number of redirects to follow
     * **/
    private final int MAX_REDIRECTS;

    private final String userAgent;

    private Socket httpSocket;

    private Map<String, String> responseHeaders;
    private int statusCode;
    private String reasonPhrase;
    private int totalRedirects;

    private boolean persistContentLength;
    private String contentLength;

    private Map<String, String> requestHeaders;

    private static final Logger LOGGER = LogManager.getLogger(HttpClient.class);

    /**
     * Creates a new HttpClient instance with the default user agent
     * **/
    public HttpClient()
    {
        this.userAgent = "Mozilla/5.0";
        this.MAX_REDIRECTS = 20;
    }

    /**
     * Creates a new HttpClient instance with a custom user agent
     * @param userAgent The user agent HttpClient will be initialized with
     * **/
    public HttpClient(String userAgent)
    {
        this.userAgent = userAgent;
        this.MAX_REDIRECTS = 20;
    }

    /**
     * Creates a new HttpClient instance with a custom user agent and custom redirect limit
     * @param userAgent The user agent HttpClient will be initialized with
     * @param maxRedirects The maximum number of redirects this HttpClient instance will follow
     * **/
    public HttpClient(String userAgent, int maxRedirects)
    {
        this.userAgent = userAgent;
        this.MAX_REDIRECTS = maxRedirects;
    }

    /**
     * Loads a public resource from a URL
     * This method is deprecated since a request model and builder were introduced
     * The simplicity of sending a simple GET request using a builder eliminates the need for this specialized method
     * @param url The URL of the resource
     * @return The input stream of the resource
     * @throws IOException If the server can't be reached / resource doesn't exist
     * **/
    @Deprecated
    public InputStream getResource(URL url) throws IOException, TooManyHttpRedirects
    {
        return sendRequest(new Request.Builder(url).build());
    }

    /**
     * Prepares and send an HTTP request
     * @param request Request to execute
     * @return The input stream of the resource
     * @throws IOException If the server can't be reached / resource doesn't exist
     * **/
    public InputStream sendRequest(Request request) throws IOException, TooManyHttpRedirects
    {
        finishRequest(); // First finish any previous request that might not have been finished by the user

        totalRedirects = 0;

        String requestString = prepareRequest(request);

        if(request.getUrl().getProtocol().toUpperCase().equals("HTTPS")) httpSocket = openSSLSocket(request.getUrl());
        else httpSocket = openSocket(request.getUrl());

        if(!httpSocket.isConnected()) throw new IOException();

        httpSocket.getOutputStream().write(requestString.getBytes());
        httpSocket.getOutputStream().flush();

        parseResponseHeader(httpSocket.getInputStream());
        if(request.shouldFollowRedirects()) handleRedirects(request);

        return makeStreamDecoder();
    }

    /**
     * Wraps the socket's InputStream into the appropriate decoder stream
     * based on the transfer encoding and content encoding.
     * @return Wrapped InputStream or null
     * **/
    private InputStream makeStreamDecoder() throws IOException
    {
        switch (getTransferEncoding())
        {
            case "identity":
                long contentLength = isConnectionCloseRequested() ? IdentityDecoderStream.READ_UNTIL_EOS : getContentLength();
                return new IdentityDecoderStream(httpSocket.getInputStream(), contentLength);

            case "chunked":
                return new ChunkedDecoderStream(httpSocket.getInputStream(), isConnectionCloseRequested());

            default: return null;
        }
    }

    private boolean isConnectionCloseRequested()
    {
        String defaultValue = requestHeaders.getOrDefault("Connection", "close");
        return responseHeaders.getOrDefault("Connection", defaultValue).toLowerCase().equals("close");
    }

    /**
     * Prepares the HTTP request by building the complete request string
     * @param request HTTP request to prepare
     * @return HTTP request string
     * **/
    private String prepareRequest(Request request)
    {
        StringBuilder resource = new StringBuilder();
        resource.append(request.getUrl().getPath());

        if(resource.length() == 0) resource.append("/");

        if(request.getUrl().getQuery() != null)
        {
            resource.append("?");
            resource.append(request.getUrl().getQuery());
        }


        StringBuilder requestString = new StringBuilder();

        requestString.append(request.getMethod().toUpperCase());
        requestString.append(" ");
        requestString.append(resource);
        requestString.append(" HTTP/");
        requestString.append(VERSION);
        requestString.append("\r\n");

        Map<String, String> headers = request.getHeaders();

        if(headers == null) headers = new HashMap<>();
        if(requestHeaders == null) requestHeaders = new HashMap<>();

        Set<String> keys = headers.keySet();

        // Normalize all header names
        for(String key : keys) requestHeaders.put(normalizeHeaderName(key), headers.get(key));

        // Set default responseHeaders
        requestHeaders.put("User-Agent", userAgent); // Force put to override any user-set values
        requestHeaders.put("Host", request.getUrl().getHost()); // Force put to override any user-set values
        requestHeaders.putIfAbsent("Accept", "*");
        requestHeaders.putIfAbsent("Accept-Charset", "*");
        requestHeaders.putIfAbsent("Accept-Encoding", "identity");
        requestHeaders.putIfAbsent("Accept-Language", "*");
        requestHeaders.putIfAbsent("Connection", "close");

        requestHeaders.forEach((key, value) -> requestString.append(key).append(": ").append(value).append("\r\n"));

        requestString.append("\r\n");

        if(request.getBody() != null)
        {
            requestString.append(new String(request.getBody()));
            requestString.append("\r\n");
        }

        return requestString.toString();
    }

    /**
     * Reads the response header and parses it
     * @param inputStream The input stream the request will be read from
     * **/
    private void parseResponseHeader(InputStream inputStream) throws IOException
    {
        String[] statusLine = StreamUtils.readLine(inputStream).split(" ");

        if(statusLine.length < 3) throw new IOException("Invalid status line");

        if(!statusLine[0].equals("HTTP/" + VERSION)) throw new UnsupportedHttpVersion(statusLine[0]);

        // Persist the content length in case we need it later
        contentLength = responseHeaders != null ? responseHeaders.getOrDefault("Content-Length", contentLength) : contentLength;

        responseHeaders = new HashMap<>();
        statusCode = Integer.parseInt(statusLine[1]);
        reasonPhrase = "";

        // Reason phrase can contain spaces so we need to reassemble all the parts
        for(int i = 2; i < statusLine.length; i++) reasonPhrase += statusLine[i] + " ";
        reasonPhrase = reasonPhrase.trim();

        String line;

        while((line = StreamUtils.readLine(inputStream)) != null && !line.equals(""))
        {
            int separatorIdx = line.indexOf(':');
            responseHeaders.put(normalizeHeaderName(line.substring(0, separatorIdx)), line.substring(separatorIdx + 1).trim());
        }

        // If we're parsing the result of a redirect and this response doesn't contain the content length
        // we should use the persisted content length
        if(persistContentLength && !responseHeaders.containsKey("Content-Length")) responseHeaders.put("Content-Length", contentLength);
    }

    /**
     * Performs a redirect in case of a 3xx response
     * @param request Request to handle redirects for
     * @throws IOException If the redirect fails
     * **/
    private void handleRedirects(Request request) throws IOException, TooManyHttpRedirects
    {
        Set<String> keys = this.responseHeaders.keySet();

        LOGGER.debug("Status code: {}", getStatusCode());
        for(String key : keys) LOGGER.debug("{}: {}", key, this.responseHeaders.get(key));

        String method = request.getMethod();
        byte[] body = request.getBody();

        switch (getStatusCode())
        {
            case 301:
            case 302:
            case 303:
                // These status codes require a new request to be made as GET
                method = "GET";
                body = null;
                break;

            case 307:
            case 308:
                // These status codes don't require any special handling but we still need
                // the case block so it doesn't catch the default block
                break;

            default:
                return;
        }

        finishRequest(); // Finish the previous request before proceeding

        if(totalRedirects >= MAX_REDIRECTS) throw new TooManyHttpRedirects();

        totalRedirects++;

        String nextLocation = this.responseHeaders.get("Location");

        URL url;

        if(nextLocation.startsWith("/")) // Redirected to just a different URI?
        {
            StringBuilder nextURLSpec = new StringBuilder();
            nextURLSpec.append(request.getUrl().getProtocol());
            nextURLSpec.append("://");
            nextURLSpec.append(request.getUrl().getHost());

            if(request.getUrl().getPort() >= 0)
            {
                nextURLSpec.append(":");
                nextURLSpec.append(request.getUrl().getPort());
            }


            nextURLSpec.append(nextLocation);

            if(request.getUrl().getQuery() != null)
            {
                nextURLSpec.append("?");
                nextURLSpec.append(request.getUrl().getQuery());
            }

            url = new URL(nextURLSpec.toString());
        }
        else if(nextLocation.toUpperCase().startsWith("HTTP")) // Redirected to a whole different URL?
        {
            url = new URL(nextLocation);
        }
        else
        {
            throw new IOException("Invalid redirect");
        }

        Request.Builder nextRequestBuilder = new Request.Builder(url);
        nextRequestBuilder.method(method).body(body).headers(request.getHeaders());

        persistContentLength = true;

        Request nextRequest = nextRequestBuilder.build();

        String requestString = prepareRequest(nextRequest);

        if(url.getProtocol().toUpperCase().equals("HTTPS")) httpSocket = openSSLSocket(url);
        else httpSocket = openSocket(url);

        if(!httpSocket.isConnected()) throw new IOException();

        httpSocket.getOutputStream().write(requestString.getBytes());
        httpSocket.getOutputStream().flush();

        parseResponseHeader(httpSocket.getInputStream());
        handleRedirects(nextRequest);
    }

    /**
     * Opens a new TCP socket to the destination
     * @param url The destination host URL
     * @return Socket connected to the destination host
     * @throws IOException If the connection fails to open
     * **/
    protected Socket openSocket(URL url) throws IOException
    {
        return new Socket(url.getHost(), urlToPort(url));
    }

    /**
     * Opens a new TCP socket with SSL
     * @param url The destination host URL
     * @return SSL Socket connected to the destionation host
     * @throws IOException If the connection fails to open
     * **/
    protected SSLSocket openSSLSocket(URL url) throws IOException
    {
        return (SSLSocket) SSLSocketFactory.getDefault().createSocket(url.getHost(), urlToPort(url));
    }

    /**
     * Returns the port that should be used to connect to the target
     * @param url The url to get the port for
     * @return Target port
     * **/
    protected int urlToPort(URL url)
    {
        int port = url.getPort();

        if(port < 0)
        {
            if(url.getProtocol().toUpperCase().equals("HTTPS")) return 443;
            return 80;
        }

        return port;
    }

    /**
     * Normalizes the header name. Everything to lower case and capital initials.
     * @param headerName The header name to be normalized
     * @return Normalized header name
     * **/
    private String normalizeHeaderName(String headerName)
    {
        String[] headerFragments = headerName.trim().split("-");

        StringBuilder stringBuilder = new StringBuilder();

        for(String fragment : headerFragments)
        {
            char initialLetter = fragment.toUpperCase().charAt(0);
            String fragmentRemainder = fragment.substring(1).toLowerCase();

            stringBuilder.append(initialLetter).append(fragmentRemainder).append("-");
        }

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }

    /**
     * Closes the connection to the server and  prepares the client for the next request
     * @throws IOException If the TCP socket can't be closed
     * **/
    public void finishRequest() throws IOException
    {
        persistContentLength = false;
        contentLength = "";

        if(httpSocket != null)
        {
            if(httpSocket.isConnected() && !httpSocket.isClosed()) httpSocket.close();
            httpSocket = null;
        }
    }

    /**
     * Returns the status code
     * @return The status code of the last request
     * **/
    public int getStatusCode()
    {
        return statusCode;
    }

    /**
     * Returns the reason phrase
     * @return The reason phrase of the last request
     * **/
    public String getReasonPhrase()
    {
        return reasonPhrase;
    }

    /**
     * Returns the user agent
     * @return User agent
     * **/
    public String getUserAgent()
    {
        return userAgent;
    }

    /**
     * Returns the response responseHeaders
     * @return The response responseHeaders of the last request
     * **/
    public Map<String, String> getResponseHeaders()
    {
        return responseHeaders;
    }

    /**
     * Returns the response content length
     * @return The response content length
     * **/
    public long getContentLength()
    {
        return Long.parseLong(responseHeaders.getOrDefault("Content-Length", "0"));
    }

    /**
     * Returns the transfer encoding
     * @return Transfer encoding header value
     * **/
    public String getTransferEncoding()
    {
        return responseHeaders.getOrDefault("Transfer-Encoding", "identity").toLowerCase();
    }
}
