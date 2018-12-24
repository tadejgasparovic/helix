package helix.system;

public class HelixSystem
{

    /**
     * Returns the normalized platform OS name
     * @return The normalized name of the OS
     * **/
    public static OSName getNormalizedSystemName()
    {
        String name = System.getProperty("os.name").toUpperCase();

        if(name.contains("NIX") || name.contains("NUX") || name.contains("AIX"))
        {
            return OSName.UNIX;
        }
        else if(name.contains("WIN"))
        {
            return OSName.WINDOWS;
        }
        else if(name.contains("MAC"))
        {
            return OSName.MAC;
        }
        /* if(name.contains("SUNOS")) // Don't support solaris for now
        {
            return OSName.SOLARIS;
        }*/
        else
        {
            return OSName.UNKNOWN;
        }
    }

    public enum OSName
    {
        UNIX, WINDOWS, MAC, SOLARIS, UNKNOWN
    }
}
