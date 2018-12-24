package helix.exceptions;

public class UnsupportedHttpVersion extends RuntimeException {

    public UnsupportedHttpVersion()
    {
        super();
    }

    public UnsupportedHttpVersion(String message)
    {
        super(message);
    }

}
