package helix.entryPoint;

import helix.system.cli.HelixCli;

public abstract class Genome
{

    public Genome()
    {
        //
    }

    /*
    * Helix genome life-cycle methods
    * */
    public abstract void onStartup();
    public abstract void onNetworkCommand(/* TODO: Pass the received command */);
    public abstract void onShutdown();

    /**
     * Should be overridden if the Genome wishes to publish any CLI commands
     * @param helixCli Command context
     * **/
    public void registerCliCommands(HelixCli helixCli){}

    /**
     * Should be overridden if the Genome wishes to publish any CLI namespaces
     * @param helixCli Namespace context
     * **/
    public void registerCliNamespaces(HelixCli helixCli){}
}
