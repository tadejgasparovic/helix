package helix.exceptions;

public class TooManyHttpRedirects extends Exception
{
    public TooManyHttpRedirects()
    {
        super();
    }

    public TooManyHttpRedirects(String message)
    {
        super(message);
    }
}
