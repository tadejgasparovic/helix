package helix.system.cli.commands;

import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.PrintStream;

public class Reload implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        if(arguments.length < 1)
        {
            usage(printStream);
            return;
        }

        if(arguments[0].equals("commands")) context.registerInstalledCommands();
        else if(arguments[0].equals("namespaces")) context.registerInstalledNamespaces();
        else usage(printStream);
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Reloads all commands or namespaces for this CLI context." +
                "\nUseful if you want to access commands or namespaces added by a newly loaded" +
                " Genome without restarting the session.");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" <commands|namespaces>");
    }

    @Override
    public String command()
    {
        return "reload";
    }
}
