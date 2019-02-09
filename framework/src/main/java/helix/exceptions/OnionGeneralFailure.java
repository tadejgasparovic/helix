package helix.exceptions;

public class OnionGeneralFailure extends Exception
{
    public OnionGeneralFailure()
    {
        super();
    }

    public OnionGeneralFailure(String message)
    {
        super(message);
    }

    public OnionGeneralFailure(Throwable throwable)
    {
        super(throwable);
    }
}
