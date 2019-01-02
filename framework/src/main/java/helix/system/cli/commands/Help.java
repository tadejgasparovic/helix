package helix.system.cli.commands;

import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;

import java.io.PrintStream;

public class Help implements CliCommand
{
    /**
     * Prints all registered commands and their descriptions or their description and usage instructions
     * based on the number of arguments.
     * @param context Context within which the command has been invoked
     * @param printStream The print stream of the context
     * @param arguments Arguments passed to the command
     * **/
    @Override
    public void execute(HelixCli context, PrintStream printStream, String ...arguments)
    {
        if(arguments.length == 0)
        {
            context.effectiveCommands().values().forEach(cliCommand -> {
                printStream.println(cliCommand.command());
                cliCommand.description(printStream);
                printStream.println();
            });
            return;
        }

        CliCommand cliCommand = context.effectiveCommands().get(arguments[0]);

        if(cliCommand == null)
        {
            printStream.println("Unknown command. Try 'help' or '?' to see a list of commands.");
            return;
        }

        cliCommand.description(printStream);
        printStream.println();
        cliCommand.usage(printStream);
    }

    /**
     * Prints the command description
     * @param printStream Print stream of the invoking context
     * **/
    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("View a list of all commands or usage instructions for a command.");
    }

    /**
     * Prints usage instructions
     * @param printStream Print stream of the invoking context
     * **/
    @Override
    public void usage(PrintStream printStream)
    {
        printStream.println("Longhand: help [command]");
        printStream.println("Shorthand: ? [command]");
    }

    /**
     * Returns the token used to invoke this command
     * @return Token used to invoke this command
     * **/
    @Override
    public String command()
    {
        return "help";
    }
}
