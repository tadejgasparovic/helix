package helix.entryPoint;

import helix.exceptions.*;
import helix.network.tor.OnionManager;
import helix.toolkit.network.HiddenHttpClient;
import helix.toolkit.network.HttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class Main
{

    public static void main(String[] args) throws InvalidGenome, IOException, UnsupportedPlatform, OnionSetupFailure, OnionGeneralFailure, TooManyHttpRedirects
    {
        /*GenomeLoader.loadFromJar("./sandbox.jar");

        GenomeLoader.startup();
        GenomeLoader.command("sandbox");
        GenomeLoader.shutdown();
        GenomeLoader.gracefulShutdown();

        System.out.println(HelixSystem.getNormalizedSystemName());
        try {
            System.out.println(bytesToHex(FileDigest.digest(new File("/home/tadej/text.png"))));
            System.out.println(bytesToHex(FileDigest.merkleRoot(new File("/home/tadej/db_migration_script/"))));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }*/

        OnionManager.init();

        HiddenHttpClient hiddenHttpClient = new HiddenHttpClient();
        InputStream inputStream = hiddenHttpClient.getResource(new URL("https://google.com?test=1#testFragment"));

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        String line;

        while((line = br.readLine()) != null) System.out.println(line);

        br.close();
    }

    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

}
