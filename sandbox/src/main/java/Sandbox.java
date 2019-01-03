import helix.entryPoint.Genome;
import helix.system.cli.HelixCli;

public class Sandbox extends Genome
{

    public Sandbox()
    {
        System.out.println("[SANDBOX] Created");
    }

    public void onStartup() {
        System.out.println("[SANDBOX] Startup");
    }

    public void onNetworkCommand() {
        System.out.println("[SANDBOX] Network command");
    }

    public void onShutdown() {
        System.out.println("[SANDBOX] Shutdown");
    }

    @Override
    public void registerCliCommands(HelixCli context)
    {
        context.registerCommand(new CustomCommand()); // Register the command to the root namespace
    }

    @Override
    public void registerCliNamespaces(HelixCli context)
    {
        context.registerNamespace(new CustomNamespace()); // Register the custom command as a part of a custom context
    }
}
