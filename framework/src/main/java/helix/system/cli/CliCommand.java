package helix.system.cli;

import java.io.PrintStream;

public interface CliCommand
{
    void execute(HelixCli context, PrintStream printStream, String ...arguments);
    void description(PrintStream printStream);
    void usage(PrintStream printStream);
    String command();
}
