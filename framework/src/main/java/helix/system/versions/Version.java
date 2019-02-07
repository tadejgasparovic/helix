package helix.system.versions;

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
     * @return < 0 if this versions precedes the <code>other</code> versions,
     * > 0 if <code>other</code> versions precedes this versions, otherwise 0
     * **/
    public abstract int compare(Version other);

    /**
     * Compares two version objects for equality
     * @param obj Version object to compare against
     * @return Flag indicating the two objects refer to the same version
     * **/
    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof Version)) throw new IllegalArgumentException();
        return compare((Version) obj) == 0;
    }
}
