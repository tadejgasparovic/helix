package helix.system.cli.commands.sequencer;

import helix.entryPoint.GenomeLoader;
import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class UninstallGenome implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        if(arguments.length < 1)
        {
            printStream.println("You need to specify at least one Genome to uninstall!");
            printStream.println("Reserved Genome name 'all' will uninstall all Genomes");
            usage(printStream);
            return;
        }

        Arrays.sort(arguments);

        try
        {
            if(Arrays.binarySearch(arguments, "all") >= 0) GenomeLoader.uninstallAllGenomes();
            else GenomeLoader.uninstallGenome(arguments);
        }
        catch (IOException e)
        {
            printStream.println("Couldn't uninstall Genome(s)!");
            printStream.println(e);
            return;
        }

        printStream.println("Genomes uninstalled!");
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Uninstalls one, multiple or all Genomes");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" <GenomeName|all> [GenomeName...]");
    }

    @Override
    public String command()
    {
        return "uninstall";
    }
}
