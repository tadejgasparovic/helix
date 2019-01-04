package helix.system.cli;

import java.io.PrintStream;

public interface CliCommand
{
    /**
     * Executes the CLI command
     * @param context Context of execution
     * @param printStream Print stream of the invoking context
     * @param arguments CLI command arguments
     * **/
    void execute(HelixCli context, PrintStream printStream, String ...arguments);

    /**
     * Prints the description of the command
     * @param printStream Print stream of the invoking context
     * **/
    void description(PrintStream printStream);

    /**
     * Prints usage instructions of the command
     * @param printStream Print stream of the invoking context
     * **/
    void usage(PrintStream printStream);

    /**
     * Returns the token used to invoke this command (can't contain spaces!)
     * @return Command token used to invoke the command
     * **/
    String command();

    /**
     * Returns TRUE if this command supports multiline input
     * @return TRUE if this command supports multiline input
     * **/
    default boolean multiline()
    {
        return false;
    }

    /**
     * The number of bytes this command is still waiting to receive (applicable when multiline() == true)
     * @return The number of bytes the command is still expecting to receive before exiting multiline mode
     * **/
    default int expectedBytes()
    {
        return 0;
    }

    /**
     * Feeds a new line of multiline data to the command and updates its state (applicable when multiline() == true)
     * @param line Line of data to feed into the command
     * @param printStream Print stream of invoking context
     * **/
    default void feedLine(String line, PrintStream printStream){}
}
