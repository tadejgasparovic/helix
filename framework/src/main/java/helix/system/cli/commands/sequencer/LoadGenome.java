package helix.system.cli.commands.sequencer;

import helix.crypto.FileDigest;
import helix.entryPoint.GenomeLoader;
import helix.exceptions.InvalidGenome;
import helix.exceptions.TooManyHttpRedirects;
import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;
import helix.toolkit.network.HttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;

public class LoadGenome implements CliCommand
{
    @Override
    public void execute(HelixCli context, PrintStream printStream, String ...arguments)
    {
        if(arguments.length < 1)
        {
            usage(printStream);
            return;
        }

        String installLocation = arguments[0];

        try
        {
            if(arguments[0].toUpperCase().startsWith("HTTP"))
            {
                URL genomeURL = new URL(arguments[0]);
                HttpClient httpClient = new HttpClient();
                InputStream inputStream = httpClient.getResource(genomeURL);

                if(httpClient.getStatusCode() / 100 != 2)
                {
                    throw new IOException("Server responded with " + httpClient.getStatusCode() + " " + httpClient.getReasonPhrase());
                }

                installLocation = GenomeLoader.GENOME_INSTALLATION_DIR.getPath();
                installLocation += File.separator;

                if(genomeURL.getFile().startsWith("/") || genomeURL.getFile().startsWith("\\"))
                {
                    installLocation += genomeURL.getFile().substring(1);
                }
                else
                {
                    installLocation += genomeURL.getFile();
                }

                Path installPath = Paths.get(installLocation);

                Files.copy(inputStream, installPath, StandardCopyOption.REPLACE_EXISTING);

                if(!installPath.toFile().exists() || installPath.toFile().length() != httpClient.getContentLength())
                {
                    throw new InvalidGenome("Genome download failed " + installPath.toFile().length() + "\t" + httpClient.getContentLength());
                }
            }

            if(arguments.length > 1)
            {
                if(arguments[1].trim().length() != 64) throw new RuntimeException("Provided SHA-256 hash is invalid");

                File genomeFile = new File(installLocation);

                if(!FileDigest.integrityCheck(genomeFile, arguments[1])) throw new InvalidGenome("Genome integrity check failed");
            }

            GenomeLoader.loadFromJar(installLocation);
            printStream.println("Genome successfully loaded!");
        }
        catch (InvalidGenome | IOException | RuntimeException | NoSuchAlgorithmException | TooManyHttpRedirects e)
        {
            printStream.println("Genome failed to load!");
            printStream.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Loads and installs a Genome from a jar file");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        printStream.print(command());
        printStream.println(" <Genome-jar-file|Genome-URL> [sha256-Genome-digest]");
    }

    @Override
    public String command()
    {
        return "load";
    }
}
