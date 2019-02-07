package helix.system.versions;

import helix.exceptions.InvalidVersionFormat;

public class SimpleVersion extends Version
{
    /**
     * Creates a new simple Version from the given string
     * A simple version contains a single integer
     * @param version String containing the single integer version number
     **/
    public SimpleVersion(String version)
    {
        super(version);
        if(!version.matches("^[0-9]+$")) throw new InvalidVersionFormat("Expected: <version>");
    }

    /**
     * Creates a new simple Version from the given integer
     * A simple version contains a single integer
     * @param version Single integer version number
     **/
    public SimpleVersion(int version)
    {
        super(Integer.toString(version));
    }

    @Override
    public int compare(Version other)
    {
        if(!(other instanceof SimpleVersion)) throw new IllegalArgumentException();
        return Integer.parseUnsignedInt(getVersion()) - Integer.parseInt(other.getVersion());
    }
}
