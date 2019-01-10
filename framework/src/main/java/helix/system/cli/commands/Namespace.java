package helix.system.cli.commands;

import helix.system.cli.CliCommand;
import helix.system.cli.CliNamespace;
import helix.system.cli.HelixCli;

import java.io.PrintStream;

public class Namespace implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        if(arguments.length < 1)
        {
            usage(printStream);
            return;
        }

        if(arguments[0].equals("list"))
        {
            context.registeredNamespaces().values().forEach(cliNamespace -> {
                printStream.println(cliNamespace.name());
                printStream.println(cliNamespace.description());
                printStream.println();
            });
        }
        else if(arguments[0].equals("set"))
        {
            if(arguments.length < 2)
            {
                usage(printStream);
                return;
            }
            CliNamespace cliNamespace = context.registeredNamespaces().get(arguments[1]);

            if(cliNamespace == null)
            {
                printStream.println("Namespace doesn't exist");
                return;
            }

            context.setActiveNamespace(cliNamespace);
        }
        else
        {
            usage(printStream);
        }
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Lets you switch between namespaces");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" [list|set] [namespace]");
    }

    @Override
    public String command()
    {
        return "namespace";
    }
}
