import helix.entryPoint.Genome;

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
}
