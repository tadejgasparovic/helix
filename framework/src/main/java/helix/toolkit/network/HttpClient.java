package helix.toolkit.network;

import helix.exceptions.UnsupportedHttpVersion;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HttpClient
{
    /**
     * HTTP version to be used in requests
     * **/
    private static final String VERSION = "1.1";

    private String userAgent;

    private Map<String, String> headers;
    private int statusCode;
    private String reasonPhrase;

    /**
     * Creates a brand new HttpClient with the default user agent
     * **/
    public HttpClient()
    {
        this.userAgent = "Mozilla/5.0";
    }

    /**
     * Creates a brand new HttpClient with a custom user agent
     * @param userAgent The user agent HttpClient will be initialized as
     * **/
    public HttpClient(String userAgent)
    {
        this.userAgent = userAgent;
    }
    /**
     * Loads a public resource from a URL
     * @param url The URL of the resource
     * @return The input stream of the resource
     * @throws IOException If the server can't be reached / resource doesn't exist
     * **/
    public InputStream getResource(URL url) throws IOException
    {
        return sendRequest(url, "GET", null, null);
    }

    /**
     * Prepares and send an HTTP request
     * @param url Resource URL
     * @param method HTTP to be used
     * @param postData HTTP POST key-value pairs
     * @param headers HTTP custom headers
     * @return The input stream of the resource
     * @throws IOException If the server can't be reached / resource doesn't exist
     * **/
    public InputStream sendRequest(URL url, String method, Map<String, String> postData, Map<String, String> headers) throws IOException
    {
        String request = prepareRequest(url, method, postData, headers);

        if(url.getProtocol().toUpperCase().equals("HTTPS"))
        {
            SSLSocket httpSocket = openSSLSocket(url);

            if(!httpSocket.isConnected()) throw new IOException();

            httpSocket.getOutputStream().write(request.getBytes());
            httpSocket.getOutputStream().flush();

            parseResponseHeader(httpSocket.getInputStream());

            return httpSocket.getInputStream();
        }
        else
        {
            Socket httpSocket = openSocket(url);

            if(!httpSocket.isConnected()) throw new IOException();

            httpSocket.getOutputStream().write(request.getBytes());
            httpSocket.getOutputStream().flush();

            parseResponseHeader(httpSocket.getInputStream());

            return httpSocket.getInputStream();
        }
    }

    /**
     * Prepares the HTTP request
     * @param url Resource URL
     * @param method HTTP to be used
     * @param postData HTTP POST key-value pairs
     * @param headers HTTP custom headers
     * @return HTTP request string
     * **/
    private String prepareRequest(URL url, String method, Map<String, String> postData, Map<String, String> headers)
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


        StringBuilder resource = new StringBuilder();
        resource.append(url.getPath());

        if(resource.length() == 0) resource.append("/");

        resource.append("?");
        resource.append(url.getQuery());


        StringBuilder request = new StringBuilder();

        request.append(method.toUpperCase());
        request.append(" ");
        request.append(resource);
        request.append(" HTTP/");
        request.append(VERSION);
        request.append("\r\n");

        if(headers == null) headers = new HashMap<>();

        // Set default headers
        headers.put("User-Agent", userAgent);
        headers.put("Connection", "close");

        headers.forEach((key, value) -> request.append(normalizeHeaderName(key)).append(": ").append(value).append("\r\n"));

        request.append("\r\n");

        request.append(body);
        request.append("\r\n");

        return request.toString();
    }

    /**
     * Reads the response header and parses it
     * @param inputStream The input stream the request will be read from
     * **/
    private void parseResponseHeader(InputStream inputStream) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        String[] statusLine = br.readLine().split(" ");

        if(statusLine.length < 3) throw new IOException("Invalid status line");

        if(!statusLine[0].equals("HTTP/" + VERSION)) throw new UnsupportedHttpVersion(statusLine[0]);

        headers = new HashMap<>();
        statusCode = Integer.parseInt(statusLine[1]);

        // Reason phrase can contain spaces so we need to reassemble all the parts
        for(int i = 2; i < statusLine.length; i++) reasonPhrase += statusLine[i] + " ";
        reasonPhrase = reasonPhrase.trim();

        String line;

        while((line = br.readLine()) != null && !line.equals(""))
        {
            String[] header = line.split(":");
            headers.put(normalizeHeaderName(header[0]), header[1].trim());
        }
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
        return Long.parseLong(headers.get("Content-Length"));
    }
}
