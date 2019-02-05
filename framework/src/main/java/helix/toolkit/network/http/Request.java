package helix.toolkit.network.http;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class Request
{
    private URL url;
    private String method;
    private Map<String, String> postData;
    private byte[] data;
    private Map<String, String> headers;

    private boolean followRedirects;

    /**
     * Creates a new request with key-value pairs for the request body
     * @param url Target URL
     * @param method Request method
     * @param postData Request body key-value pairs
     * @param headers Custom request headers
     * @param followRedirects Flag, if true the HTTP client will follow redirects until max limit or 2xx status code
     * **/
    public Request(URL url, String method, Map<String, String> postData, Map<String, String> headers, boolean followRedirects)
    {
        this.url = url;
        this.method = method;
        this.postData = postData;
        this.headers = headers;
        this.followRedirects = followRedirects;
    }

    /**
     * Creates a new request with bytes for the request body
     * @param url Target URL
     * @param method Request method
     * @param data Request body bytes
     * @param headers Custom request headers
     * @param followRedirects Flag, if true the HTTP client will follow redirects until max limit or 2xx status code
     * **/
    public Request(URL url, String method, byte[] data, Map<String, String> headers, boolean followRedirects)
    {
        this.url = url;
        this.method = method;
        this.data = data;
        this.headers = headers;
        this.followRedirects = followRedirects;
    }

    /**
     * Target URL getter
     * @return Target URLs
     * **/
    public URL getUrl()
    {
        return url;
    }

    /**
     * Request method getter
     * @return Request method
     * **/
    public String getMethod()
    {
        return method;
    }

    /**
     * Request body getter
     * If the body are key-value pairs they're URL encoded and converted to bytes
     * @return Request body
     * **/
    public byte[] getBody()
    {
        if(data != null) return data;

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

        return body.toString().getBytes();
    }

    /**
     * Custom headers getter
     * @return Map of custom headers
     * **/
    public Map<String, String> getHeaders()
    {
        return headers;
    }

    /**
     * followRedirects flag getter
     * @return Flag indicating if the HTTP client should follow redirects
     * **/
    public boolean shouldFollowRedirects()
    {
        return followRedirects;
    }

    public static class Builder
    {
        private URL url;
        private String method;
        private Map<String, String> postData;
        private byte[] data;
        private Map<String, String> headers;
        private boolean followRedirects;

        /**
         * Creates a new request builder for a request to the specified URL.
         * Sets all other parameters to their default values
         * @param url This request's target URL
         * **/
        public Builder(URL url)
        {
            this.url = url;
            this.method = "GET";
            this.postData = null;
            this.data = null;
            this.headers = null;
            this.followRedirects = true;
        }

        /**
         * Sets the request method
         * @param method Request method
         * @return Instance of this builder for chaining
         * **/
        public Builder method(String method)
        {
            this.method = method;
            return this;
        }

        /**
         * Sets the request body (overrides any previously set binary body)
         * @param postData Request body key-value pairs
         * @return Instance of this builder for chaining
         * **/
        public Builder body(Map<String, String> postData)
        {
            this.data = null;
            this.postData = postData;
            return this;
        }

        /**
         * Sets the binary request body (overrides any previously set post body key-value pairs)
         * @param data Binary data to be sent as the request body
         * @return Instance of this builder for chaining
         * **/
        public Builder body(byte[] data)
        {
            this.postData = null;
            this.data = data;
            return this;
        }

        /**
         * Sets the request headers
         * @param headers Custom HTTP headers
         * @return Instance of this builder for chaining
         * **/
        public Builder headers(Map<String, String> headers)
        {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the followRedirects flag
         * @param followRedirects Flag, if true the HTTP client will follow redirects until max limit or 2xx status code
         * @return Instance of this builder for chaining
         * **/
        public Builder followRedirects(boolean followRedirects)
        {
            this.followRedirects = followRedirects;
            return this;
        }

        /**
         * Builds the request
         * @return Built request
         * **/
        public Request build()
        {
            if(data != null) return new Request(url, method, data, headers, followRedirects);
            return new Request(url, method, postData, headers, followRedirects);
        }
    }
}
