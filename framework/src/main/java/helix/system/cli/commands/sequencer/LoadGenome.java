package helix.system.cli.commands.sequencer;

import helix.entryPoint.GenomeLoader;
import helix.exceptions.InvalidGenome;
import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.IOException;
import java.io.PrintStream;

public class LoadGenome implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String ...arguments)
    {
        if(arguments.length != 1)
        {
            usage(printStream);
            return;
        }

        try
        {
            GenomeLoader.loadFromJar(arguments[0]);
            printStream.println("Genome successfully loaded!");
        }
        catch (InvalidGenome | IOException e)
        {
            printStream.println("Genome failed to load!");
            printStream.println(e.getMessage());
        }
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Loads a Genome from a jar file");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" <Genome_jar_file>");
    }

    @Override
    public String command()
    {
        return "load";
    }
}
