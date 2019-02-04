package helix.entryPoint;

import helix.exceptions.InvalidGenome;
import helix.system.cli.HelixCli;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class GenomeLoader
{
    public static final File GENOME_INSTALLATION_DIR; // TODO: Obfuscate dir name

    private static Map<String, Genome> loadedGenomes;

    private static final Logger LOGGER = LogManager.getLogger(GenomeLoader.class);

    static
    {
        loadedGenomes = new HashMap<>();

        File canonicalInstallationDir;
        try
        {
            canonicalInstallationDir = new File("./genomes/").getCanonicalFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            canonicalInstallationDir = null;
        }
        GENOME_INSTALLATION_DIR = canonicalInstallationDir;

        if(GENOME_INSTALLATION_DIR == null) throw new RuntimeException("Genome installation dir couldn't be resolved!");

        if(!GENOME_INSTALLATION_DIR.exists()) GENOME_INSTALLATION_DIR.mkdir();
    }

    /**
     * Tries to load a Genome from a JAR file
     * @param filename The path to the JAR file
     * @throws InvalidGenome If the JAR file doesn't contain a valid Genome
     * @throws IOException If the Genome JAR file couldn't be installed
     * **/
    public static String loadFromJar(String filename) throws InvalidGenome, IOException
    {
        File file = new File(filename);

        if(!file.exists() || !file.isFile()) throw new InvalidGenome();

        URL fileURL;
        try {
            fileURL = new URL("file://" + file.getCanonicalPath());
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

        if(loadedGenomes.containsKey(name)) throw new InvalidGenome("Genome already loaded");

        if(!file.getAbsoluteFile().getPath().startsWith(GENOME_INSTALLATION_DIR.getPath()))
        {
            String installation = config.containsKey("installation") ? config.getProperty("installation") : "copy";

            switch (installation)
            {
                case "copy":
                    String installLocation = GENOME_INSTALLATION_DIR.getPath() + File.separator + file.getName();
                    Files.copy(Paths.get(filename), Paths.get(installLocation));
                    return loadFromJar(installLocation);

                case "move":
                    installLocation = GENOME_INSTALLATION_DIR.getPath() + File.separator + file.getName();
                    Files.move(Paths.get(filename), Paths.get(installLocation));
                    return loadFromJar(installLocation);

                case "keep":
                    break;

                default:
                    throw new InvalidGenome("Invalid 'installation' config value");
            }
        }


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

        if(genomeClass.getClass().getSuperclass().equals(Genome.class))
        {
            return injectGenome(genomeClass, config);
        }
        throw new InvalidGenome();
    }

    /**
     * Injects a Genome directly from a class instead of loading it from a JAR file.
     * Useful for embedded payloads which reside inside the same JAR file as the Helix framework
     * @param clazz Class to attempt to instantiate a Genome
     * @param config Configuration properties containing at least the name of the Genome
     * @return The name of the injected Genome
     * @throws NullPointerException If any of the arguments are null
     * @throws InvalidGenome If the Genome injection fails
     * **/
    public static String injectGenome(Class<? extends Genome> clazz, Properties config) throws NullPointerException, InvalidGenome
    {
        if(clazz == null || config == null) throw new NullPointerException();

        String name = config.getProperty("name");

        if(name.length() < 1) throw new InvalidGenome();

        try
        {
            Genome genome = clazz.newInstance();
            genome.onStartup();
            loadedGenomes.put(name, genome);
            return name;
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new InvalidGenome();
        }
    }

    /**
     * Forwards a command to a specific Genome
     * @param genomeId The genome the command will be sent to
     * @throws InvalidGenome if the Genome doesn't exist
     * **/
    public static void networkCommand(String genomeId/* TODO: Command */) throws InvalidGenome
    {
        Genome genome = loadedGenomes.get(genomeId);
        if(genome == null) throw new InvalidGenome();

        genome.onNetworkCommand(/* TODO: Pass the command */);
    }

    /**
     * Broadcasts a command to all loaded Genomes
     * **/
    public static void broadcastNetworkCommand(/* TODO: Command */)
    {
        loadedGenomes.values().forEach(Genome::onNetworkCommand);
    }

    /**
     * Invokes the onShutdown() Genome life-cycle method
     * **/
    public static void shutdown()
    {
        loadedGenomes.values().forEach(Genome::onShutdown);
    }

    /**
     * Invokes the registerCliCommands() Genome life-cycle method
     * @param helixCli Command context
     * **/
    public static void registerCliCommands(HelixCli helixCli)
    {
        loadedGenomes.values().forEach(genome -> genome.registerCliCommands(helixCli));
    }

    /**
     * Invokes the registerCliNamespaces() Genome life-cycle method
     * @param helixCli Namespace context
     * **/
    public static void registerCliNamespaces(HelixCli helixCli)
    {
        loadedGenomes.values().forEach(genome ->genome.registerCliNamespaces(helixCli));
    }

    /**
     * Returns a set of all loaded Genome IDs
     * @return Loaded genome IDs
     * **/
    public static Set<String> loadedGenomes()
    {
        return loadedGenomes.keySet();
    }

    /**
     * Loads all Genomes in the Genome installation directory
     * **/
    public static void loadInstalledGenomes()
    {
        LOGGER.debug("Loading installed Genomes...");
        File[] genomeCandidates = GENOME_INSTALLATION_DIR.listFiles();

        if(genomeCandidates == null) return;

        for(File candidate : genomeCandidates)
        {
            String[] nameParts = candidate.getName().split("\\.");
            if(!nameParts[nameParts.length - 1].trim().equals("jar")) return;

            try
            {
                LOGGER.debug("Loading candidate {}...", candidate.getPath());
                loadFromJar(candidate.getPath());
                LOGGER.debug("Candidate {} loaded successfully!");
            }
            catch (InvalidGenome | IOException e)
            {
                LOGGER.debug("Candidate failed to load!", e);
            }
        }

    }

    /**
     * Shuts down and unloads all Genomes
     * **/
    public static void unloadAllGenomes()
    {
        shutdown();
        loadedGenomes.clear();
    }

    /**
     * Unloads one or more Genomes
     * @param genomes Genome names
     * **/
    public static void unloadGenome(String ...genomes)
    {
        for(String genomeName : genomes)
        {
            Genome genome = loadedGenomes.get(genomeName);
            if(genome == null) continue;

            genome.onShutdown();
            loadedGenomes.remove(genomeName);
        }
    }

    /**
     * Unloads and uninstalls all Genomes
     * @throws IOException If a Genome fails to uninstall
     * **/
    public static void uninstallAllGenomes() throws IOException
    {
        uninstallGenome(loadedGenomes.keySet().toArray(new String[0]));
    }

    /**
     * Unloads and uninstalls one or more Genomes
     * @param genomes Genome names
     * @throws IOException If a Genome fails to uninstall
     * **/
    public static void uninstallGenome(String ...genomes) throws IOException
    {
        for(String genomeName : genomes)
        {
            Genome genome = loadedGenomes.get(genomeName);
            if(genome == null) continue;

            unloadGenome(genomeName);

            CodeSource codeSource = genome.getClass().getProtectionDomain().getCodeSource();

            if(codeSource == null) continue;

            Files.deleteIfExists(Paths.get(codeSource.getLocation().getPath()));
        }
    }
}
