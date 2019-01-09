package helix.system.cli.commands.sequencer;

import helix.entryPoint.GenomeLoader;
import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.PrintStream;
import java.util.Arrays;

public class UnloadGenome implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        if(arguments.length < 1)
        {
            printStream.println("You need to specify at least one Genome to unload!");
            printStream.println("Reserved Genome name 'all' will unload all genomes");
            usage(printStream);
            return;
        }

        Arrays.sort(arguments);

        if(Arrays.binarySearch(arguments, "all") >= 0) GenomeLoader.unloadGenomes();
        else GenomeLoader.unloadGenome(arguments);

        printStream.println("Genomes unloaded!");
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Unloads one, multiple or all Genomes");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" <genomeName|all> [genomeName...]");
    }

    @Override
    public String command()
    {
        return "unload";
    }
}
