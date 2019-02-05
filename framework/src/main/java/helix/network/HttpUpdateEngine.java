package helix.network;

import helix.exceptions.OnionGeneralFailure;
import helix.network.tor.OnionManager;
import helix.toolkit.network.http.HiddenHttpClient;
import helix.toolkit.network.http.HttpClient;

import java.io.InputStream;
import java.net.URL;

public abstract class HttpUpdateEngine
{
    private URL endpoint;
    private HttpClient httpClient;

    /**
     * Creates a new update engine which uses the given endpoint to check for and download updates
     * @param endpoint Update endpoint
     * @param clearnetAllowed Can we fallback to a clearnet connection if Tor fails?
     * **/
    public HttpUpdateEngine(URL endpoint, boolean clearnetAllowed)
    {
        this.endpoint = endpoint;

        try
        {
            if(OnionManager.isTor()) httpClient = new HiddenHttpClient();
            else httpClient = new HttpClient();
        } catch (OnionGeneralFailure onionGeneralFailure)
        {
            httpClient = new HttpClient();
        }
    }

    /**
     * Checks the current version against the version returned by the API
     * **/
    public boolean versionCheck(String currentVersion)
    {
        // TODO
        return false;
    }

    /**
     * Reads and parses the update server's response
     * **/
    protected abstract String readVersion(InputStream response);
}
