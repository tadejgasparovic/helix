package helix.entryPoint;

import helix.exceptions.InvalidGenome;
import helix.system.HelixKernel;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGenomeInjection extends Genome
{
    @Override
    public void onStartup()
    {
        System.out.println("[INJECTED] Startup");
    }

    @Override
    public void onNetworkCommand()
    {
        //
    }

    @Override
    public void onShutdown()
    {
        System.out.println("[INJECTED] Shutdown");
    }

    @Test
    public void doesSuccessfullyInjectGenome() throws InvalidGenome, InterruptedException
    {
        HelixKernel.bootstrap(new String[0]);

        Properties config = new Properties();
        config.put("name", "injected");

        assertEquals(GenomeLoader.injectGenome(this.getClass(), config), "injected");

        HelixKernel.destroy();
    }
}
