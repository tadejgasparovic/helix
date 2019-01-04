package helix.system.cli.namespaces;

import helix.system.cli.CliNamespace;
import helix.system.cli.commands.network.Http;

public class Network extends CliNamespace
{
    public Network()
    {
        registerCommand(new Http());
    }

    @Override
    public String description()
    {
        return "Network toolkit";
    }

    @Override
    public String name()
    {
        return "network";
    }
    //
}
