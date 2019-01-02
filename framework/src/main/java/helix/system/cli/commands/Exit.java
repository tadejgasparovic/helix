package helix.system.cli.commands;

import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.PrintStream;

public class Exit implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        if(context.getActiveNamespace() != null) context.setActiveNamespace(null);
        else context.close();
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Switch back to the root context or terminate the CLI");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.println(command());
    }

    @Override
    public String command()
    {
        return "exit";
    }
}
