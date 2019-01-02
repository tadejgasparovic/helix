package helix.entryPoint;

public abstract class Genome {

    public Genome()
    {
        //
    }

    /*
    * Helix genome life-cycle methods
    * */
    public abstract void onStartup();
    public abstract void onCommand(/* TODO: Pass the received command */);
    public abstract void onShutdown();

}
