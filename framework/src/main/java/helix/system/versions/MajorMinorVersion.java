package helix.system.versions;

import helix.exceptions.InvalidVersionFormat;

public class MajorMinorVersion extends Version
{
    /**
     * Creates a new major.minor Version from the given string
     * A major.minor version contains two integers separated by a dot (.).
     * The first and second number represent the major and minor versions respectively
     * @param version String containing the version
     **/
    public MajorMinorVersion(String version)
    {
        super(version);
        if(!version.matches("^[0-9]+\\.[0-9]+$")) throw new InvalidVersionFormat("Expected <major>.<minor>");
    }

    /**
     * Creates a new major.minor Version from the given string
     * A major.minor version contains two integers separated by a dot (.).
     * The first and second number represent the major and minor versions respectively
     * @param major Major version number
     * @param minor Minor version number
     **/
    public MajorMinorVersion(int major, int minor)
    {
        super(major + "." + minor);
    }

    /**
     * Getter for the major version
     * @return Integer containing the major version
     * **/
    public int major()
    {
        return Integer.parseInt(getVersion().split("\\.")[0]);
    }

    /**
     * Getter for the minor version
     * @return Integer containing the minor version
     * **/
    public int minor()
    {
        return Integer.parseInt(getVersion().split("\\.")[1]);
    }

    @Override
    public int compare(Version other)
    {
        if(!(other instanceof MajorMinorVersion)) throw new IllegalArgumentException();

        MajorMinorVersion otherVersion = (MajorMinorVersion) other;

        int majorDiff = major() - otherVersion.major();

        return (majorDiff == 0) ? (minor() - otherVersion.minor()) : majorDiff;
    }
}
