package helix.system.cli.commands.sequencer;

import helix.entryPoint.GenomeLoader;
import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.PrintStream;

public class UnloadGenome implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        if(arguments.length < 1)
        {
            printStream.println("You need to specify at least one Genome to unload!");
            return;
        }

        GenomeLoader.unloadGenome(arguments);
        printStream.println("Genomes unloaded!");
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Unloads one or more Genomes");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" <genomeName> [...]");
    }

    @Override
    public String command()
    {
        return "unload";
    }
}
