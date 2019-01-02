package helix.system.cli;

import helix.entryPoint.GenomeLoader;
import helix.system.cli.commands.Help;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HelixCli extends Thread
{
    private InputStream inputStream;
    private PrintStream outputStream;

    private Map<String, CliCommand> commands;

    private volatile boolean running;

    /**
     * Creates a new HelixCli instance bound to stdio
     * **/
    public HelixCli()
    {
        inputStream = System.in;
        outputStream = System.out;
        running = true;
        commands = new HashMap<>();
        registerInstalledCommands();
    }

    /**
     * Creates a new HelixCli instance bound to the passed InputStream and PrintStream
     * @param in The input stream to bind to
     * @param out The print stream to bind to
     * **/
    public HelixCli(InputStream in, PrintStream out)
    {
        inputStream = in;
        outputStream = out;
        running = true;
        commands = new HashMap<>();
        registerInstalledCommands();
    }

    /**
     * Creates a new HelixCli instance bound to the passed InputStream and PrintStream
     * @param in The input stream to bind to
     * @param out The output stream to bind to
     * **/
    public HelixCli(InputStream in, OutputStream out)
    {
        inputStream = in;
        outputStream = new PrintStream(out);
        running = true;
        commands = new HashMap<>();
        registerInstalledCommands();
    }

    /**
     * Registers Helix system commands as well as commands from all loaded Genomes
     * **/
    private void registerInstalledCommands()
    {
        registerCommand(new Help());

        GenomeLoader.registerCliCommands(this);
    }

    /**
     * Reads and parses user input in a separate thread to avoid blocking the main thread
     * **/
    @Override
    public void run()
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        try {
            printHeader();
        } catch (IOException e) {
            //
        }

        try
        {
            String line = null;

            do
            {
                outputStream.println();
                if(line != null) execute(line);

                outputStream.print("Helix>");
            }
            while((line = br.readLine()) != null && running);
        }
        catch (Exception e)
        {
            close();
            outputStream.println("Helix CLI reader thread crashed. Restart the CLI session to retry.");
            e.printStackTrace(outputStream);
        }
        finally
        {
            try
            {
                br.close();
            }
            catch (IOException e)
            {
                // IGNORE
            }
        }
    }

    /**
     * Parses and executes a CLI command
     * @param command Command to be parsed and executed
     * **/
    public void execute(String command)
    {
        String[] commandTokens = command.split(" ");

        if(commandTokens[0].equals("help") || commandTokens[0].equals("?"))
        {
            CliCommand cliCommand = commands.get("help");

            if(cliCommand == null)
            {
                outputStream.println("Derp...");
                return;
            }

            cliCommand.execute(this, outputStream, Arrays.copyOfRange(commandTokens, 1, commandTokens.length));
            return;
        }

        CliCommand cliCommand = commands.get(commandTokens[0]);

        if(cliCommand == null)
        {
            outputStream.println("Unknown command. Try 'help' or '?' to see a list of commands.");
            return;
        }

        execute(cliCommand, Arrays.copyOfRange(commandTokens, 1, commandTokens.length));
    }

    /**
     * Executes the CLI command with the given arguments
     * @param cliCommand CLI command to execute
     * @param arguments Arguments to execute the CLI command with
     * **/
    public void execute(CliCommand cliCommand, String ...arguments)
    {
        if(cliCommand == null) return;
        cliCommand.execute(this, outputStream, arguments);
    }

    /**
     * Registers one or more CLI commands and makes them available to the user
     * @param cliCommands List of commands to register
     * **/
    public void registerCommand(CliCommand ...cliCommands)
    {
        for(CliCommand cliCommand : cliCommands) commands.put(cliCommand.command(), cliCommand);
    }

    /**
     * Prints the HELIX banner to the output print stream
     * @throws IOException If the banner can't be loaded
     * **/
    private void printHeader() throws IOException
    {
        InputStream inputStream = HelixCli.class.getClassLoader().getResourceAsStream("banner");

        int b;

        while((b = inputStream.read()) > -1) outputStream.write(b);
        outputStream.println();
        outputStream.println();

        inputStream.close();
    }

    /**
     * Closes the development CLI
     * **/
    public void close()
    {
        this.running = false;
    }

    /**
     * Getter for all registered commands
     * @return A map of all commands
     * **/
    public Map<String, CliCommand> registeredCommands()
    {
        return commands;
    }
}
