package helix.exceptions;

public class InvalidVersionFormat extends RuntimeException
{
    public InvalidVersionFormat()
    {
        super();
    }

    public InvalidVersionFormat(String message)
    {
        super(message);
    }
}
