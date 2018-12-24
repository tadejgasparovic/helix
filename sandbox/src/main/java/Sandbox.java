import helix.entryPoint.Genome;

public class Sandbox extends Genome {

    public Sandbox()
    {
        System.out.println("[SANDBOX] Created");
    }

    public void onStartup() {
        System.out.println("[SANDBOX] Startup");
    }

    public void onCommand() {
        System.out.println("[SANDBOX] Command");
    }

    public void onShutdown() {
        System.out.println("[SANDBOX] Shutdown");
    }

    public void onGracefulShutdown() {
        System.out.println("[SANDBOX] Graceful shutdown");
    }
}
