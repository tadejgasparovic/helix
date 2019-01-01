package helix.network.tor;

import helix.crypto.FileDigest;
import helix.exceptions.OnionGeneralFailure;
import helix.exceptions.OnionSetupFailure;
import helix.exceptions.TooManyHttpRedirects;
import helix.exceptions.UnsupportedPlatform;
import helix.system.HelixSystem;
import helix.toolkit.files.FileUtils;
import helix.toolkit.network.HttpClient;
import helix.toolkit.zip.Zip;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class OnionManager
{

    /*
    * File system
    * */
    private static final String TOR_ROOT = "./tor";
    private static final File TOR_ROOT_DIR = new File(TOR_ROOT);
    private static final String TOR_INTEGRITY_STORE = "./.tor.hash";

    /*
    * Tor proxy
    * */
    private static Proxy proxy;
    private static final String proxyHost = "127.0.0.1";
    private static final int proxyPort = 9050;

    private static Process process;

    private static final int STARTUP_FAILURE_LIMIT = 10;

    /*
    * Installation
    * */
    private static final int SETUP_FAILURE_LIMIT = 10;
    private static final HelixSystem.OSName[] SUPPORTED_PLATFORMS = new HelixSystem.OSName[]
    {
            HelixSystem.OSName.WINDOWS,
            HelixSystem.OSName.UNIX
    };

    // TODO: Dinamically load the latest version
    private static final String TOR_DOWNLOAD_WINDOWS = "https://www.torproject.org/dist/torbrowser/8.0.3/tor-win32-0.3.4.8.zip";
    private static final String TOR_DOWNLOAD_UNIX = null; // TODO: Support Unix system downloads; for now assume Tor is already installed
    private static final String TOR_DOWNLOAD_MAC = null; // TODO: Support Mac system downloads; for now assume Tor is already installed

    private static final Logger LOGGER = LogManager.getLogger(OnionManager.class);

    /**
     * Sets up Tor (if necessary) and starts it up
     * @throws OnionGeneralFailure Tor couldn't be started
     * @throws UnsupportedPlatform if the detected platform isn't supported
     * @throws OnionSetupFailure if Tor couldn't be setup
     * **/
    public static void init() throws OnionSetupFailure, UnsupportedPlatform, OnionGeneralFailure
    {
        if(!isPlatformSupported()) throw new UnsupportedPlatform();

        int setupFailures;

        LOGGER.debug("Checking tor setup and installing if necessary...");

        // Try to setup tor
        for(setupFailures = 0; (setupFailures < SETUP_FAILURE_LIMIT) && !isSetup(); setupFailures++) setup();

        LOGGER.debug("Setup failures: {}", setupFailures);

        if(setupFailures == SETUP_FAILURE_LIMIT)
        {
            // Even if the counter is maxed out the last try might've been a success
            if(!isSetup()) throw new OnionSetupFailure();
        }

        LOGGER.debug("Tor setup / integrity check succeeded! Attempting startup...");

        int startupFailures;

        for(startupFailures = 0; startupFailures < STARTUP_FAILURE_LIMIT; startupFailures++)
        {
            try {
                createProcess();

                if(isTor()) break; // Check if we can connect to the proxy

            } catch (OnionGeneralFailure e) {
                e.printStackTrace();
            }
        }

        LOGGER.debug("Startup failures: {}", startupFailures);

        if(startupFailures + 1 == STARTUP_FAILURE_LIMIT)
        {
            if(!isTor()) throw new OnionGeneralFailure();
        }

        LOGGER.debug("Tor is setup and running");
    }

    /**
     * Attempts to start a tor process
     * @throws UnsupportedPlatform if the detected platform is unsupported
     * @throws OnionGeneralFailure if the process / proxy creation fails
     * **/
    private static void createProcess() throws UnsupportedPlatform, OnionGeneralFailure
    {
        if(getTorDownload() == null)
        {
            LOGGER.debug("Using system Tor installation...");
            proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
            return; // If there's no binary to download, then there's nothing to start
        }
        try {
            LOGGER.debug("Starting Tor...");
            process = Runtime.getRuntime().exec(getTorCommand());
            proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
        } catch (IOException e) {
            LOGGER.error("Tor process couldn't be created", e);
            throw new OnionGeneralFailure();
        }
    }

    /**
     * Checks the Tor setup
     * @return true if the setup is correct
     * @throws OnionSetupFailure if setup check fails
     * **/
    private static boolean isSetup() throws OnionSetupFailure
    {
        if(getTorDownload() == null) return true; // For some platforms we just assume Tor is already present on the system

        if(!TOR_ROOT_DIR.exists() || !TOR_ROOT_DIR.isDirectory()) return false;

        LOGGER.debug("Checking the integrity of the installation...");

        // Check the binaries
        if(integrityCheck()) return true;

        LOGGER.error("Integrity check failed...");

        return false;
    }

    /**
     * Attempts to connect to the Tor proxy and figure out if the service running on that port
     * is indeed Tor. We don't want to flood a random service with SOCKS5 proxy requests.
     * @return true of the tested service is Tor
     * @throws OnionGeneralFailure if the test couldn't be completed
     * **/
    private static boolean isTor() throws OnionGeneralFailure
    {
        try {
            LOGGER.debug("Attempting Tor service IDENT...");

            Socket testSocket = new Socket(proxyHost, proxyPort);

            if(!testSocket.isConnected())
            {
                LOGGER.debug("Tor service: NOT DETECTED");
                return false; // No service detected running on proxyHost:proxyPort
            }

            // If we can connect to the proxy we can further try to identify
            // the service by sending a dummy HTTP proxy request which Tor
            // would usually reject with
            // HTTP/1.0 501 Tor is not an HTTP Proxy

            // Write the dummy request to the service
            testSocket.getOutputStream().write("CONNECT google.com HTTP/1.1".getBytes());
            testSocket.getOutputStream().flush();

            // Read the first line from the response and search it for the word "Tor"
            BufferedReader br = new BufferedReader(new InputStreamReader(testSocket.getInputStream()));
            String responseLine = br.readLine();
            br.close();

            testSocket.close();

            LOGGER.debug("Tor service: {}", (responseLine.toUpperCase().contains("TOR") ? "IDENT" : "NOT DETECTED"));

            return responseLine.toUpperCase().contains("TOR");

        } catch (IOException e) {
            throw new OnionGeneralFailure();
        }
    }

    /**
     * Checks if the current platform is supported
     * @return True of the current platform is supported
     * **/
    private static boolean isPlatformSupported()
    {
        HelixSystem.OSName currentPlatform = HelixSystem.getNormalizedSystemName();

        boolean platformSupported = false;

        for(int i = 0; i < SUPPORTED_PLATFORMS.length; i++)
        {
            if(SUPPORTED_PLATFORMS[i].equals(currentPlatform))
            {
                platformSupported = true;
                break;
            }
        }

        return platformSupported;
    }

    /**
     * Calculates and verifies the integrity of the installation
     * @return true if the installation is valid
     * @throws OnionSetupFailure if integrity check fails
     * **/
    private static boolean integrityCheck() throws OnionSetupFailure
    {
        File integrityStore = new File(TOR_INTEGRITY_STORE);

        if(!integrityStore.exists() || integrityStore.length() != 32 || integrityStore.isDirectory()) return false;

        byte[] merkleRoot;
        byte[] referenceHash;

        try
        {
            merkleRoot = FileDigest.merkleRoot(TOR_ROOT_DIR); // TODO: Only read files that don't change over time (i.e. ignore logs)
            referenceHash = Files.readAllBytes(Paths.get(integrityStore.toURI()));
        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            throw new OnionSetupFailure("Merkle root");
        }

        if(Arrays.equals(merkleRoot, referenceHash)) return true;

        integrityStore.delete();
        TOR_ROOT_DIR.delete();

        return false;
    }

    /**
     * Downloads and installs Tor binaries
     * @throws OnionSetupFailure If the setup fails
     * **/
    private static void setup() throws OnionSetupFailure
    {
        String downloadURL = getTorDownload();

        // Not necessary, but good practice so we don't accidentally try to download from a `null` URL
        if(downloadURL == null) return;

        try {
            LOGGER.debug("Downloading Tor binaries...");

            if(TOR_ROOT_DIR.exists()) FileUtils.deleteDirectoryRecursively(TOR_ROOT_DIR);

            File binaries = downloadBinaries(new URL(downloadURL));

            if(binaries == null) throw new OnionSetupFailure("Couldn't download binaries");

            // TODO: Verify integrity

            LOGGER.debug("Extracting files...");
            Zip.extract(binaries, TOR_ROOT_DIR);

            LOGGER.debug("Calculating merkle root...");
            byte[] merkleRoot = FileDigest.merkleRoot(TOR_ROOT_DIR);
            Files.write(Paths.get(TOR_INTEGRITY_STORE), merkleRoot);

            LOGGER.debug("Setup finished. Integrity store created.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new OnionSetupFailure(e.getMessage());
        }
    }


    /**
     * Opens a plain-text socket connection over the Tor network. Avoids DNS leaking.
     * @param address The destination address.
     * @param port The destination port.
     * @return The opened socket ready for read / write operations.
     * @throws IOException On Socket.connect() failure.
     **/
    public static Socket openSocket(String address, int port) throws IOException
    {
        LOGGER.debug("Opening plain text socket to {}:{}...", address, port);
        Socket socket = new Socket(proxy);
        InetSocketAddress addr = InetSocketAddress.createUnresolved(address, port);
        socket.connect(addr);
        return socket;
    }

    /**
     * Opens a secure SSL socket connection over the Tor network. Avoids DNS leaking.
     * @param address The destination address.
     * @param port The destination port.
     * @return The opened socket ready for read / write operations.
     * @throws IOException On Socket.connect() failure.
     **/
    public static SSLSocket openSSLSocket(String address, int port) throws IOException
    {

        Socket socket = openSocket(address, port);

        LOGGER.debug("Wrapping socket to {}:{} with SSL...", address, port);
        return (SSLSocket) ((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(socket, proxyHost, proxyPort, true);
    }

    /**
     * Assembles the system-specific command to start Tor
     * @return The system-specific command
     * @throws UnsupportedPlatform When the detected platform isn't supported
     * **/
    private static String getTorCommand() throws UnsupportedPlatform
    {
        switch(HelixSystem.getNormalizedSystemName())
        {
            case WINDOWS:
                return TOR_ROOT_DIR.getAbsolutePath() + "/tor.exe";

            default:
                throw new UnsupportedPlatform();
        }
    }

    /**
     * Returns the platform-specific Tor download URL or null if the platform
     * isn't supported or if it should be assumed Tor is already present on the platform
     * @return Tor download URL or null
     * **/
    private static String getTorDownload()
    {
        switch (HelixSystem.getNormalizedSystemName())
        {
            case WINDOWS: return TOR_DOWNLOAD_WINDOWS;
            default: return null;
        }
    }

    /**
     * Downloads the binaries and returns the downloaded file or null in case of a failure
     * @param url Download URL
     * @return Downloaded binaries file
     * @throws IOException If we're unable to download the file
     * @throws OnionSetupFailure If the downloaded file fails the integrity check
     * **/
    private static File downloadBinaries(URL url) throws IOException, OnionSetupFailure, TooManyHttpRedirects
    {
        HttpClient httpClient = new HttpClient();

        InputStream inputStream = httpClient.getResource(url);

        LOGGER.debug("Status Code: {}", httpClient.getStatusCode());

        if(httpClient.getStatusCode() / 100 != 2) return null;

        Path path = Files.createTempFile("onion", ".zip");

        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING); // Download the file

        inputStream.close();

        File file = path.toFile();
        file.deleteOnExit();

        // Simple integrity check
        if(file.length() != httpClient.getContentLength()) throw new OnionSetupFailure("Download failed");

        return file;
    }

}
