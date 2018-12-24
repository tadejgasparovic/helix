package helix.entryPoint;

import helix.exceptions.InvalidGenome;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class GenomeLoader
{

    private static Map<String, Genome> loadedGenomes;

    static
    {
        loadedGenomes = new HashMap<>();
    }

    /**
     * Tries to load a genome from a JAR file
     * @param filename The path to the JAR file
     * @throws InvalidGenome If the JAR file doesn't contain a valid genome
     * **/
    public static void loadFromJar(String filename) throws InvalidGenome
    {
        File file = new File(filename);

        if(!file.exists() || !file.isFile()) throw new InvalidGenome();

        URL fileURL;
        try {
            fileURL = new URL("file://" + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            throw new InvalidGenome();
        }
        URLClassLoader jarFile = new URLClassLoader(new URL[]{ fileURL }, GenomeLoader.class.getClassLoader());

        InputStream applicationConfig = jarFile.getResourceAsStream("helix.conf");

        if(applicationConfig == null) throw new InvalidGenome(); // Not a helix application

        Properties config = new Properties();
        try {
            config.load(applicationConfig);
        } catch (IOException e) {
            throw new InvalidGenome();
        }

        if(!config.containsKey("main") || !config.containsKey("name")) throw new InvalidGenome();

        String main = config.getProperty("main");
        String name = config.getProperty("name");

        Class genomeClass;
        try
        {
            genomeClass = Class.forName(main, true, jarFile);
            if(genomeClass == null || name.length() < 1) throw new InvalidGenome();
        }
        catch (ClassNotFoundException e)
        {
            throw new InvalidGenome();
        }

        try
        {
            Genome genome = (Genome) genomeClass.newInstance();
            loadedGenomes.put(name, genome);
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new InvalidGenome();
        }
    }

    /**
     * Invokes the onStartup() genome life-cycle method
     * **/
    public static void startup()
    {
        loadedGenomes.values().forEach(Genome::onStartup);
    }

    /**
     * Forwards a command to a specific genome
     * @param genomeId The genome the command will be sent to
     * @throws InvalidGenome if the genome doesn't exist
     * **/
    public static void command(String genomeId/* TODO: Command */) throws InvalidGenome
    {
        Genome genome = loadedGenomes.get(genomeId);
        if(genome == null) throw new InvalidGenome();

        genome.onCommand(/* TODO: Pass the command */);
    }

    /**
     * Broadcasts a command to all loaded genomes
     * **/
    public static void broadcastCommand(/* TODO: Command */)
    {
        loadedGenomes.values().forEach(Genome::onCommand);
    }

    /**
     * Calls the onShutdown() genome life-cycle method
     * **/
    public static void shutdown()
    {
        loadedGenomes.values().forEach(Genome::onShutdown);
    }

    /**
     * Calls the onGracecfulShutdown() genome life-cycle method
     * **/
    public static void gracefulShutdown()
    {
        loadedGenomes.values().forEach(Genome::onGracefulShutdown);
    }

    /**
     * Returns a set of all loaded genome IDs
     * @return Loaded genome IDs
     * **/
    public static Set<String> loadedGenomes()
    {
        return loadedGenomes.keySet();
    }

}
