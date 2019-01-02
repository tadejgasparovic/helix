package helix.system.cli.commands.sequencer;

import helix.entryPoint.GenomeLoader;
import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.PrintStream;

public class ListGenomes implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        GenomeLoader.loadedGenomes().forEach(printStream::println);
        printStream.println();
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Lists all loaded Genomes");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.println(command());
    }

    @Override
    public String command()
    {
        return "list";
    }
}
