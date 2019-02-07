package helix.network;

import helix.exceptions.UpdateFailure;
import helix.system.versions.Version;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class HelixUpdateEngine extends HttpUpdateEngine
{
    public HelixUpdateEngine(Version currentVersion, boolean clearnetAllowed) throws UpdateFailure
    {
        super(currentVersion, clearnetAllowed);
    }

    @Override
    protected Version readVersion(InputStream response)
    {
        return null; // TODO: Read and parse JSON
    }

    @Override
    protected boolean doUpdate(InputStream response)
    {
        // Files.copy(response, Paths.get("...")); // TODO: Download and install
        return false;
    }

    @Override
    protected URL getVersionCheckEndpoint()
    {
        try
        {
            return new URL(""); // TODO: Return correct URL. Read from config?
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }

    @Override
    protected URL getUpdateEndpoint()
    {
        return null; // TODO: Return correct URL
    }
}
