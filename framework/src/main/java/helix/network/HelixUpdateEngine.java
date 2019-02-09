package helix.network;

import helix.exceptions.InvalidVersionFormat;
import helix.exceptions.UpdateFailure;
import helix.system.Config;
import helix.system.versions.MajorMinorPatchVersion;
import helix.system.versions.Version;
import helix.toolkit.network.http.HttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HelixUpdateEngine extends HttpUpdateEngine
{
    private URL updateURL;

    public HelixUpdateEngine(Version currentVersion, boolean clearnetAllowed) throws UpdateFailure
    {
        super(currentVersion, clearnetAllowed);
    }

    @Override
    protected Version readVersion(HttpClient context, InputStream response) throws UpdateFailure
    {
        Map<String, String> headers = context.getResponseHeaders();

        // If the response isn't a JSON object something went wrong
        if(!headers.getOrDefault("Content-Type", "").contains("application/json"))
        {
            throw new UpdateFailure("Expected content type application/json but got" + headers.get("Content-Type"));
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(response));

        StringBuilder jsonString = new StringBuilder();

        String line;

        try
        {
            while((line = br.readLine()) != null) jsonString.append(line);
        }
        catch(IOException e)
        {
            throw new UpdateFailure(e);
        }

        try
        {
            JSONObject jsonObject = new JSONObject(jsonString.toString());

            String version = jsonObject.getString("latest");
            String updateURL = jsonObject.getString("download");

            if(version == null || updateURL == null) throw new UpdateFailure("Version or download URL null!");

            this.updateURL = new URL(updateURL);

            return new MajorMinorPatchVersion(version);
        }
        catch (JSONException | MalformedURLException | InvalidVersionFormat e)
        {
            throw new UpdateFailure(e);
        }
    }

    @Override
    protected boolean doUpdate(HttpClient context, InputStream response) throws UpdateFailure
    {
        // Files.copy(response, Paths.get("...")); // TODO: Download and install
        return false;
    }

    @Override
    protected URL getVersionCheckEndpoint()
    {
        try
        {
            String versionCheck = Config.instance().getProperty("versionCheck");

            if(versionCheck == null) return null;

            return new URL(versionCheck);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    @Override
    protected URL getUpdateEndpoint()
    {
        return updateURL;
    }
}
