package helix.system;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

public class DevelopmentCli extends Thread
{
    private InputStream inputStream;
    private PrintStream outputStream;

    private volatile boolean running;

    private final Logger LOGGER = LogManager.getLogger(DevelopmentCli.class);

    /**
     * Creates a new DevelopmentCli instance bound to stdio
     * **/
    public DevelopmentCli()
    {
        inputStream = System.in;
        outputStream = System.out;
        running = true;
    }

    /**
     * Creates a new DevelopmentCli instance bound to the passed InputStream and PrintStream
     * @param in The input stream to bind to
     * @param out The print stream to bind to
     * **/
    public DevelopmentCli(InputStream in, PrintStream out)
    {
        inputStream = in;
        outputStream = out;
        running = true;
    }

    /**
     * Creates a new DevelopmentCli instance bound to the passed InputStream and PrintStream
     * @param in The input stream to bind to
     * @param out The output stream to bind to
     * **/
    public DevelopmentCli(InputStream in, OutputStream out)
    {
        inputStream = in;
        outputStream = new PrintStream(out);
        running = true;
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
                outputStream.print("Helix>");

                if(line == null) continue;

                // TODO: Parse and execute command
            }
            while((line = br.readLine()) != null && running);
        }
        catch (Exception e)
        {
            close();
            outputStream.println("Helix development CLI reader thread crashed. Restart Helix to retry");
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
     * Prints the HELIX banner to the output print stream
     * @throws IOException If the banner can't be loaded
     * **/
    private void printHeader() throws IOException
    {
        InputStream inputStream = DevelopmentCli.class.getClassLoader().getResourceAsStream("banner");

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
}
