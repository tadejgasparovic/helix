import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.PrintStream;

public class CustomCommand implements CliCommand
{
    public void execute(HelixCli helixCli, PrintStream printStream, String ...arguments)
    {
        if(arguments.length < 1)
        {
            printStream.println("Hello Helix!");
            return;
        }

        for(String argument : arguments) printStream.println(argument);
    }

    public void description(PrintStream printStream)
    {
        printStream.println("Custom command added by the Genome sandbox as a demonstration");
    }

    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" [*]");
    }

    public String command()
    {
        return "custom-command";
    }
}
