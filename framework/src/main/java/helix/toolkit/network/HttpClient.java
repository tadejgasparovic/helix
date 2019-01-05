package helix.toolkit.network;

import helix.exceptions.TooManyHttpRedirects;
import helix.exceptions.UnsupportedHttpVersion;
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

    private Map<String, String> headers;
    private int statusCode;
    private String reasonPhrase;
    private int totalRedirects;

    private boolean persistContentLength;
    private String contentLength;

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
     * @param url The URL of the resource
     * @return The input stream of the resource
     * @throws IOException If the server can't be reached / resource doesn't exist
     * **/
    public InputStream getResource(URL url) throws IOException, TooManyHttpRedirects
    {
        return sendRequest(url, "GET", (byte[]) null, null, true);
    }

    /**
     * Prepares and send an HTTP request
     * @param url Resource URL
     * @param method HTTP to be used
     * @param postData HTTP POST key-value pairs
     * @param headers HTTP custom headers
     * @param followRedirects If TRUE the HttpClient will automatically follow redirects
     * @return The input stream of the resource
     * @throws IOException If the server can't be reached / resource doesn't exist
     * **/
    public InputStream sendRequest(URL url, String method, Map<String, String> postData, Map<String, String> headers, boolean followRedirects) throws IOException, TooManyHttpRedirects
    {
        boolean hasContent = postData != null && postData.size() > 0;

        StringBuilder body = new StringBuilder();

        if(hasContent)
        {
            postData.forEach((key, value) -> {
                try {
                    body.append(URLEncoder.encode(key, "UTF-8"));
                    body.append("=");
                    body.append(URLEncoder.encode(value, "UTF-8"));
                    body.append("&");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            });

            body.deleteCharAt(body.length() - 1);
        }

        return sendRequest(url, method, body.toString().getBytes(), headers, followRedirects);
    }

    /**
     * Prepares and send an HTTP request
     * @param url Resource URL
     * @param method HTTP to be used
     * @param postData HTTP request body content
     * @param headers HTTP custom headers
     * @param followRedirects If TRUE the HttpClient will automatically follow redirects
     * @return The input stream of the resource
     * @throws IOException If the server can't be reached / resource doesn't exist
     * **/
    public InputStream sendRequest(URL url, String method, byte[] postData, Map<String, String> headers, boolean followRedirects) throws IOException, TooManyHttpRedirects
    {
        finishRequest(); // First finish any previous request that might not have been finished by the user

        totalRedirects = 0;

        String request = prepareRequest(url, method, postData, headers);

        if(url.getProtocol().toUpperCase().equals("HTTPS")) httpSocket = openSSLSocket(url);
        else httpSocket = openSocket(url);

        if(!httpSocket.isConnected()) throw new IOException();

        httpSocket.getOutputStream().write(request.getBytes());
        httpSocket.getOutputStream().flush();

        parseResponseHeader(httpSocket.getInputStream());
        if(followRedirects) handleRedirects(url, method, postData, headers);

        return httpSocket.getInputStream();
    }

    /**
     * Prepares the HTTP request
     * @param url Resource URL
     * @param method HTTP request method to be used
     * @param postData HTTP request body content
     * @param headers HTTP custom headers
     * @return HTTP request string
     * **/
    private String prepareRequest(URL url, String method, byte[] postData, Map<String, String> headers)
    {
        StringBuilder resource = new StringBuilder();
        resource.append(url.getPath());

        if(resource.length() == 0) resource.append("/");

        if(url.getQuery() != null)
        {
            resource.append("?");
            resource.append(url.getQuery());
        }


        StringBuilder request = new StringBuilder();

        request.append(method.toUpperCase());
        request.append(" ");
        request.append(resource);
        request.append(" HTTP/");
        request.append(VERSION);
        request.append("\r\n");

        if(headers == null) headers = new HashMap<>();

        // Set default headers
        headers.putIfAbsent("User-Agent", userAgent);
        headers.putIfAbsent("Host", url.getHost());
        headers.putIfAbsent("Accept", "*");
        headers.putIfAbsent("Accept-Charset", "*");
        headers.putIfAbsent("Accept-Encoding", "identity");
        headers.putIfAbsent("Accept-Language", "*");
        headers.putIfAbsent("Connection", "close");

        headers.forEach((key, value) -> request.append(normalizeHeaderName(key)).append(": ").append(value).append("\r\n"));

        request.append("\r\n");

        if(postData != null)
        {
            request.append(new String(postData));
            request.append("\r\n");
        }

        return request.toString();
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
        contentLength = headers != null ? headers.getOrDefault("Content-Length", contentLength) : contentLength;

        headers = new HashMap<>();
        statusCode = Integer.parseInt(statusLine[1]);
        reasonPhrase = "";

        // Reason phrase can contain spaces so we need to reassemble all the parts
        for(int i = 2; i < statusLine.length; i++) reasonPhrase += statusLine[i] + " ";
        reasonPhrase = reasonPhrase.trim();

        String line;

        while((line = StreamUtils.readLine(inputStream)) != null && !line.equals(""))
        {
            int separatorIdx = line.indexOf(':');
            headers.put(normalizeHeaderName(line.substring(0, separatorIdx)), line.substring(separatorIdx + 1).trim());
        }

        // If we're parsing the result of a redirect and this response doesn't contain the content length
        // we should use the persisted content length
        if(persistContentLength && !headers.containsKey("Content-Length")) headers.put("Content-Length", contentLength);
    }

    /**
     * Performs a redirect in case of a 3xx response
     * @param url Last request URL
     * @param method Last request method
     * @param postData Last request post data
     * @param headers Last request headers
     * @throws IOException If the redirect fails
     * **/
    private void handleRedirects(URL url, String method, byte[] postData, Map<String, String> headers) throws IOException, TooManyHttpRedirects
    {
        Set<String> keys = this.headers.keySet();

        LOGGER.debug("Status code: {}", getStatusCode());
        for(String key : keys) LOGGER.debug("{}: {}", key, this.headers.get(key));

        switch (getStatusCode())
        {
            case 301:
            case 302:
            case 303:
                // These status codes require a new request to be made as GET
                method = "GET";
                postData = null;
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

        String nextLocation = this.headers.get("Location");

        if(nextLocation.startsWith("/")) // Redirected to just a different URI?
        {
            StringBuilder nextURLSpec = new StringBuilder();
            nextURLSpec.append(url.getProtocol());
            nextURLSpec.append("://");
            nextURLSpec.append(url.getHost());

            if(url.getPort() >= 0)
            {
                nextURLSpec.append(":");
                nextURLSpec.append(url.getPort());
            }


            nextURLSpec.append(nextLocation);

            if(url.getQuery() != null)
            {
                nextURLSpec.append("?");
                nextURLSpec.append(url.getQuery());
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

        persistContentLength = true;

        String request = prepareRequest(url, method, postData, headers);

        if(url.getProtocol().toUpperCase().equals("HTTPS")) httpSocket = openSSLSocket(url);
        else httpSocket = openSocket(url);

        if(!httpSocket.isConnected()) throw new IOException();

        httpSocket.getOutputStream().write(request.getBytes());
        httpSocket.getOutputStream().flush();

        parseResponseHeader(httpSocket.getInputStream());
        handleRedirects(url, method, postData, headers);
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
     * Returns the response headers
     * @return The response headers of the last request
     * **/
    public Map<String, String> getHeaders()
    {
        return headers;
    }

    /**
     * Returns the response content length
     * @return The response content length
     * **/
    public long getContentLength()
    {
        return Long.parseLong(headers.getOrDefault("Content-Length", "0"));
    }

    /**
     * Returns the transfer encoding
     * @return Transfer encoding header value
     * **/
    public String getTransferEncoding()
    {
        return headers.getOrDefault("Transfer-Encoding", "identity");
    }
}
