package helix.system.cli.commands.network;

import helix.exceptions.TooManyHttpRedirects;
import helix.system.cli.CliCommand;
import helix.system.cli.HelixCli;
import helix.toolkit.network.HttpClient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Http implements CliCommand
{
    private boolean multiline = false;

    private HttpClient httpClient;
    private int expectedBytes;


    private String method;
    private URL url;
    private Map<String, String> headers;
    private StringBuilder requestBody;
    private boolean followRedirects;
    private boolean forceDownload;

    public Http()
    {
        headers = new HashMap<>();
    }

    @Override
    public void execute(HelixCli context, PrintStream printStream, String... arguments)
    {
        if(arguments.length < 2)
        {
            usage(printStream);
            return;
        }

        method = arguments[0].toUpperCase();
        try
        {
            url = new URL(arguments[1]);
        }
        catch (MalformedURLException e)
        {
            printStream.println("Malformed URL");
            return;
        }

        String[] options = Arrays.copyOfRange(arguments, 2, arguments.length);

        httpClient = null;
        multiline = false;
        expectedBytes = 0;
        headers.clear();
        requestBody = null;
        followRedirects = true;
        forceDownload = false;

        for(String option : options)
        {
            if(option.startsWith("-a"))
            {
                httpClient = new HttpClient(option.substring(2, option.length() - 2));
                break;
            }
        }

        if(httpClient == null) httpClient = new HttpClient();

        for(String option : options)
        {
            if(option.startsWith("-H")) parseHeader(option.substring(2), printStream);

            if(option.startsWith("-b"))
            {
                if(expectedBytes > 0 || multiline) // That's not right... Can't use -b if we already used -l
                {
                    printStream.println("You can't use -l and -b at the same time");
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder();

                for(int i = Arrays.asList(options).indexOf("-b") + 1; i < options.length; i++)
                {
                    stringBuilder.append(options[i]).append(" ");
                }

                stringBuilder.deleteCharAt(stringBuilder.length() - 1);

                requestBody = stringBuilder;
            }

            if(option.startsWith("-c")) headers.put("Content-Type", option.substring(3, option.length() - 2));

            if(option.startsWith("-l"))
            {
                if(requestBody != null) // Can't use -b and -l together
                {
                    printStream.println("You can't use -l and -b at the same time");
                    return;
                }
                multiline = true;
                expectedBytes = Integer.parseInt(option.substring(2));
                requestBody = new StringBuilder();
            }

            if(option.equals("-d")) forceDownload = true;
        }

        if(!multiline)
        {
            try
            {
                finishRequest(printStream);
            }
            catch (IOException | TooManyHttpRedirects e)
            {
                printStream.print("Request failed. Error: ");
                printStream.println(e.getMessage());
                printStream.println("Stack trace:");
                e.printStackTrace(printStream);
            }
        }
    }

    /**
     * Sends and finishes the request
     * @param printStream Invoking context print stream
     * **/
    private void finishRequest(PrintStream printStream) throws IOException, TooManyHttpRedirects
    {
        InputStream inputStream = httpClient.sendRequest(url, method, requestBody != null ? requestBody.toString().getBytes() : null, headers, followRedirects);

        if(httpClient.getStatusCode() / 100 != 2)
        {
            printStream.print("HTTP request failed with status code ");
            printStream.println(httpClient.getStatusCode());
            httpClient.finishRequest();
            return;
        }

        String contentType = httpClient.getResponseHeaders().get("Content-Type");

        if(contentType.startsWith("text/") && !forceDownload)
        {
            int read;

            while((read = inputStream.read()) > -1) printStream.write(read);

            printStream.println();
            printStream.flush();
            inputStream.close();
        }
        else
        {
            String filename = (url.getFile().startsWith("/") ? url.getFile().substring(1) : url.getFile());

            if(filename.length() < 1) filename = "index"; // TODO: Map Content-Type to file extension

            Path downloadPath = Paths.get("./" + filename);

            Files.createFile(downloadPath);

            File f = downloadPath.toFile();

            FileOutputStream fos = new FileOutputStream(f);

            int read;

            while((read = inputStream.read()) > -1) fos.write(read);

            fos.flush();
            fos.close();

            //Files.copy(inputStream, downloadPath);

            inputStream.close();
        }

        httpClient.finishRequest();
    }

    @Override
    public void feedLine(String line, PrintStream printStream)
    {
        if((expectedBytes - line.length()) < 0)
        {
            requestBody.append(line.substring(0, expectedBytes));
            try
            {
                finishRequest(printStream);
            }
            catch (IOException | TooManyHttpRedirects e)
            {
                printStream.print("Request failed. Error: ");
                printStream.println(e.getMessage());
                printStream.println("Stack trace:");
                e.printStackTrace(printStream);
            }
            return;
        }

        requestBody.append(line);
        expectedBytes -= line.length();

        if(expectedBytes == 0)
        {
            try
            {
                finishRequest(printStream);
            }
            catch (IOException | TooManyHttpRedirects e)
            {
                printStream.print("Request failed. Error: ");
                printStream.println(e.getMessage());
                printStream.println("Stack trace:");
                e.printStackTrace(printStream);
            }
        }
    }

    private void parseHeader(String header, PrintStream printStream)
    {
        String[] headerFragments = header.split("=");

        if(headerFragments.length < 2)
        {
            printStream.print("Invalid header ");
            printStream.println(header);
            return;
        }

        headers.put(headerFragments[0].trim(), headerFragments[1].trim());
    }

    @Override
    public void description(PrintStream printStream)
    {
        printStream.println("Lets you to make HTTP requests");
    }

    @Override
    public void usage(PrintStream printStream)
    {
        // TODO: Add options for using different interfaces (Tor, VPN, clearnet)
        printStream.print(command());
        printStream.println(" <request-method> <url> [options]");
        printStream.println();
        printStream.println("Options:");
        printStream.println("-H<key>=<value>\tSets custom header 'key' to value 'value'");
        printStream.println("-b<request-body>\tRequest body (MUST be the last used option!)");
        printStream.println("-c\"<content-type>\"\tContent type");
        printStream.println("-a\"<agent>\"\tUser agent");
        printStream.println("-l<content-length>\tSets the request body content length. If this option is set you can use new lines in the request body.");
        printStream.println("  \tYou can't use -l and -b together!");
        printStream.println("-n\tDon't follow redirects");
    }

    @Override
    public String command()
    {
        return "http";
    }

    @Override
    public boolean multiline()
    {
        return multiline;
    }

    @Override
    public int expectedBytes()
    {
        return expectedBytes;
    }
}
