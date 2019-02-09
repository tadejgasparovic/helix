package helix.system.versions;

import helix.exceptions.InvalidVersionFormat;

public class MajorMinorPatchVersion extends Version
{
    /**
     * Creates a new major.minor.patch Version from the given string
     * A major.minor.patch version contains three integers separated by a dot (.).
     * The first, second and third number represent the major, minor and patch versions respectively
     * @param version String containing the version
     * @throws InvalidVersionFormat If the version string doesn't conform to the expecteds format
     **/
    public MajorMinorPatchVersion(String version) throws InvalidVersionFormat
    {
        super(version);
        if(!version.matches("^[0-9]+\\.[0-9]+\\.[0-9]+$")) throw new InvalidVersionFormat("Expected <major>.<minor>.<patch>");
    }

    /**
     * Creates a new major.minor.patch Version from the given string
     * A major.minor.patch version contains three integers separated by a dot (.).
     * The first, second and third number represent the major, minor and patch versions respectively
     * @param major Major version number
     * @param minor Minor version number
     * @param patch Patch version number
     **/
    public MajorMinorPatchVersion(int major, int minor, int patch)
    {
        super(major + "." + minor + "." + patch);
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

    /**
     * Getter for the patch version
     * @return Integer containing the patch version
     * **/
    public int patch()
    {
        return Integer.parseInt(getVersion().split("\\.")[2]);
    }

    @Override
    public int compare(Version other)
    {
        if(!(other instanceof MajorMinorPatchVersion)) throw new IllegalArgumentException();

        MajorMinorPatchVersion otherVersion = (MajorMinorPatchVersion) other;

        int majorDiff = major() - otherVersion.major();
        int minorDiff = minor() - otherVersion.minor();

        return (majorDiff == 0) ? (minorDiff == 0 ? (patch() - otherVersion.patch()) : minorDiff) : majorDiff;
    }
}
