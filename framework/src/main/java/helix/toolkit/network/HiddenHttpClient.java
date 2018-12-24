package helix.toolkit.network;

import helix.network.tor.OnionManager;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;

public class HiddenHttpClient extends HttpClient
{
    @Override
    protected Socket openSocket(URL url) throws IOException
    {
        return OnionManager.openSocket(url.getHost(), urlToPort(url));
    }

    @Override
    protected SSLSocket openSSLSocket(URL url) throws IOException
    {
        return OnionManager.openSSLSocket(url.getHost(), urlToPort(url));
    }
}
