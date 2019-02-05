package helix.exceptions;

public class UpdateFailure extends Exception
{
    public UpdateFailure()
    {
        super();
    }

    public UpdateFailure(String message)
    {
        super(message);
    }

    public UpdateFailure(Throwable throwable)
    {
        super(throwable);
    }
}
