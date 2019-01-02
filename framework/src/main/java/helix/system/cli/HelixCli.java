package helix.system.cli;

import helix.entryPoint.GenomeLoader;
import helix.system.cli.commands.Exit;
import helix.system.cli.commands.Help;
import helix.system.cli.commands.Namespace;
import helix.system.cli.namespaces.GenomeSequencer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HelixCli extends Thread
{
    private InputStream inputStream;
    private PrintStream outputStream;

    private Map<String, CliCommand> commands;
    private Map<String, CliNamespace> namespaces;

    private CliNamespace activeNamespace = null;

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
        namespaces = new HashMap<>();
        registerInstalledCommands();
        registerInstalledNamespaces();
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
        namespaces = new HashMap<>();
        registerInstalledCommands();
        registerInstalledNamespaces();
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
        namespaces = new HashMap<>();
        registerInstalledCommands();
        registerInstalledNamespaces();
    }

    /**
     * Registers Helix system commands as well as commands from all loaded Genomes
     * **/
    private void registerInstalledCommands()
    {
        registerCommand(new Help());
        registerCommand(new Namespace());
        registerCommand(new Exit());

        GenomeLoader.registerCliCommands(this);
    }

    /**
     * Registers Helix system namespaces as well as namespaces from all loaded Genomes
     * **/
    private void registerInstalledNamespaces()
    {
        registerNamespace(new GenomeSequencer());

        GenomeLoader.registerCliNamespaces(this);
    }

    /**
     * Reads and parses user input in a separate thread to avoid blocking the main thread
     * **/
    @Override
    public void run()
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        try
        {
            printHeader();
        }
        catch (IOException e)
        {
            //
        }

        try
        {
            String line = null;

            do
            {
                outputStream.println();
                if(line != null && !line.equals("")) execute(line);

                if(activeNamespace == null) outputStream.print("Helix>");
                else outputStream.print("Helix(" + activeNamespace.name() + ")>");
            }
            while((line = br.readLine()) != null && running);
        }
        catch (Exception e)
        {
            if(!running) return; // If the crash was caused by close() then we should ignore it
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

        CliCommand cliCommand = effectiveCommands().get(commandTokens[0]);

        if(cliCommand == null)
        {
            outputStream.println("Unknown command. Try 'help' or '?' to see a list of commands.");
            return;
        }

        execute(cliCommand, Arrays.copyOfRange(commandTokens, 1, commandTokens.length));
    }

    /**
     * Returns a map of namespace-specific effective commands.
     * @return Map of effective commands
     * **/
    public Map<String, CliCommand> effectiveCommands()
    {
        if(activeNamespace == null) return commands;
        return activeNamespace.commands();
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
     * Registers one or more CLI namespaces and makes them available to the user
     * @param cliNamespaces List of commands to register
     * **/
    public void registerNamespace(CliNamespace ...cliNamespaces)
    {
        for(CliNamespace cliNamespace : cliNamespaces) namespaces.put(cliNamespace.name(), cliNamespace);
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
     * Closes the Helix CLI
     * **/
    public void close()
    {
        this.running = false;
        try
        {
            inputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace(outputStream);
        }
    }

    /**
     * Getter for all registered commands
     * @return A map of all commands
     * **/
    public Map<String, CliCommand> registeredCommands()
    {
        return commands;
    }

    /**
     * Getter for all registered namespaces
     * @return A map of all namespaces
     * **/
    public Map<String, CliNamespace> registeredNamespaces()
    {
        return namespaces;
    }

    /**
     * Getter for the active namespace
     * @return Active namespace in this context
     * **/
    public CliNamespace getActiveNamespace() {
        return activeNamespace;
    }

    /**
     * Setter for the active namespace in this context
     * @param activeNamespace Next active namespace
     * **/
    public void setActiveNamespace(CliNamespace activeNamespace) {
        this.activeNamespace = activeNamespace;
    }
}
