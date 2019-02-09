package helix.network;

import helix.exceptions.OnionGeneralFailure;
import helix.exceptions.TooManyHttpRedirects;
import helix.exceptions.UpdateFailure;
import helix.network.tor.OnionManager;
import helix.system.versions.Version;
import helix.toolkit.network.http.HiddenHttpClient;
import helix.toolkit.network.http.HttpClient;
import helix.toolkit.network.http.Request;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class HttpUpdateEngine implements Runnable
{
    private HttpClient httpClient;

    private volatile Version currentVersion;

    private int intervalSec;

    private Thread workerThread;

    /**
     * Creates a new update engine which uses the given endpoint to check for and download updates
     * @param currentVersion Currently running versions of the software
     * @param clearnetAllowed Can we fallback to a clearnet connection if Tor fails?
     * @throws UpdateFailure If Tor isn't available and clearnet isn't allowed
     * **/
    public HttpUpdateEngine(Version currentVersion, boolean clearnetAllowed) throws UpdateFailure
    {
        intervalSec = -1; // Single check
        this.currentVersion = currentVersion;

        try
        {
            if(OnionManager.isTor()) httpClient = new HiddenHttpClient();
            else if(clearnetAllowed) httpClient = new HttpClient();
            else throw new UpdateFailure("Tor not available and clearnet not allowed!");
        }
        catch (OnionGeneralFailure onionGeneralFailure)
        {
            if(clearnetAllowed) httpClient = new HttpClient();
            else throw new UpdateFailure(onionGeneralFailure);
        }
    }

    /**
     * Starts the update engine in a separate thread
     * @throws RuntimeException If the update engine is already running
     * **/
    public void start()
    {
        if(workerThread != null) throw new RuntimeException("Update engine already running");

        workerThread = new Thread(this);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Stops the update engine thread. Blocks for up to 1000ms
     * @throws InterruptedException If the thread <code>stop()</code> was called on is interrupted
     * **/
    public void stop() throws InterruptedException
    {
        if(workerThread == null) throw new RuntimeException("Update engine not running");

        workerThread.interrupt();
        workerThread.join(1000); // Wait for the worker thread to exit

        workerThread = null;
    }

    /**
     * Runs the update engine in a separate thread (Shouldn't be called explicitly!)
     * **/
    @Override
    public void run()
    {
        if(workerThread == null || !workerThread.isAlive())
        {
            throw new RuntimeException("HttpUpdateEngine.run() shouldn't be called explicitly!" +
                                        "Use HttpUpdateEngine.start() instead");
        }

        while(!workerThread.isInterrupted())
        {
            try
            {
                attemptUpdate();
            }
            catch (UpdateFailure updateFailure)
            {
                updateFailure.printStackTrace();
            }

            if(intervalSec < 0) break;

            try
            {
                Thread.sleep(intervalSec * 1000);
            }
            catch (InterruptedException e)
            {
                // IGNORE
            }
        }
    }

    /**
     * Sets the update check interval
     * @param intervalSec Interval in seconds (-1 to only run a single check)
     * **/
    public void setInterval(int intervalSec)
    {
        this.intervalSec = intervalSec;
    }

    /**
     * Attempts an automatic update if there is a new versions available
     * @return Flag, true if update was successfully performed
     * @throws UpdateFailure If the update attempt fails
     * **/
    public boolean attemptUpdate() throws UpdateFailure
    {
        if(!versionCheck(currentVersion)) return false; // Already running the latest versions

        URL updateEndpoint = getUpdateEndpoint();

        if(updateEndpoint == null) throw new UpdateFailure("No update endpoint");

        Request request = buildUpdateRequest(updateEndpoint);

        try
        {
            InputStream response = httpClient.sendRequest(request);
            return doUpdate(httpClient, response);
        }
        catch (IOException | TooManyHttpRedirects e)
        {
            throw new UpdateFailure(e);
        }
    }

    /**
     * Checks the current versions against the versions returned by the API
     * @param currentVersion Current software versions
     * @return true if an update is available, otherwise false
     * @throws UpdateFailure If the versions check fails
     * **/
    public boolean versionCheck(Version currentVersion) throws UpdateFailure
    {
        URL versionCheckEndpoint = getVersionCheckEndpoint();

        if(versionCheckEndpoint == null) throw new UpdateFailure("No versions check endpoint");

        Request request = buildVersionCheckRequest(versionCheckEndpoint);

        InputStream response;
        try
        {
            response = httpClient.sendRequest(request);
        }
        catch (IOException | TooManyHttpRedirects e)
        {
            throw new UpdateFailure(e);
        }

        if(response == null) throw new UpdateFailure("Version check failed");

        Version availableVersion = readVersion(httpClient, response);

        if(availableVersion == null) throw new UpdateFailure("Version check failed. Version is null");

        return currentVersion.compare(availableVersion) < 0;
    }

    /**
     * Reads and parses the update server's response
     * @param context Request context / HttpClient
     * @param response InputStream starting at the beginning of the response body
     * @return Latest available versions
     * @throws UpdateFailure If the latest version cannot be parsed from the response
     * **/
    protected abstract Version readVersion(HttpClient context, InputStream response) throws UpdateFailure;

    /**
     * Reads and installs the update returned by the server
     * @param context Request context / HttpClient
     * @param response InputStream starting at the beginning of the response body
     * @return Flag indicating update success
     * @throws UpdateFailure If the latest version cannot be downloaded / installed
     * **/
    protected abstract boolean doUpdate(HttpClient context, InputStream response) throws UpdateFailure;

    /**
     * Returns the URL of the versions check endpoint. Using a method to return the URL can prove useful for cases where
     * the URL needs to be generated on-the-fly
     * @return URL of the versions check endpoint
     * **/
    protected abstract URL getVersionCheckEndpoint();

    /**
     * Returns the URL of the latest update. Using a method to return the URL can prove useful for cases where
     * the URL needs to be generated on-the-fly or parsed from the response received by <code>readVersion()</code>
     * @return URL of the latest update
     * **/
    protected abstract URL getUpdateEndpoint();

    /**
     * Builds the request used in the update download. Can be overridden to modify the request and allow for things
     * such as authorization, etc.
     * @param url URL to send the request to. Usually return value of <code>getUpdateEndpoint()</code>
     * @return Built request
     * **/
    protected Request buildUpdateRequest(URL url)
    {
        Request.Builder requestBuilder = new Request.Builder(url);
        return requestBuilder.build();
    }

    /**
     * Builds the request used in the versions check. Can be overridden to modify the request and allow for things
     * such as authorization, etc.
     * @param url URL to send the request to. Usually return value of <code>getVersionCheckEndpoint()</code>
     * @return Built request
     * **/
    protected Request buildVersionCheckRequest(URL url)
    {
        Request.Builder requestBuilder = new Request.Builder(url);
        return requestBuilder.build();
    }

    /**
     * Current software versions getter
     * @return Current versions
     * **/
    public Version getCurrentVersion()
    {
        return currentVersion;
    }

    /**
     * Current software versions setter
     * @param currentVersion New software versions
     * **/
    public void setCurrentVersion(Version currentVersion)
    {
        this.currentVersion = currentVersion;
    }
}
