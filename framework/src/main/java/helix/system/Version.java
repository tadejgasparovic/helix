package helix.system;

public abstract class Version
{
    private String version;

    /**
     * Creates a new Version from the given string
     * **/
    public Version(String version)
    {
        this.version = version;
    }

    /**
     * Version string getter
     * @return Version string
     * **/
    public String getVersion()
    {
        return version;
    }

    /**
     * Compares two versions
     * @param other Version to compare against
     * @return -1 if this version precedes the <code>other</code> version,
     * 1 if <code>other</code> version precedes this version, otherwise 0
     * **/
    public abstract int compare(Version other);
}
