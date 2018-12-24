package helix.crypto;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileDigest {

    public static final byte[] EMPTY_HASH = new byte[32];

    /**
     * Digests a file using SHA-256
     * @param file The file to be digested
     * @return SHA-256 hash
     * @throws IOException If the file doesn't exist or couldn't be read
     * @throws NoSuchAlgorithmException If SHA-256 wasn't found on the system
     * **/
    public static byte[] digest(File file) throws IOException, NoSuchAlgorithmException
    {
        if(!file.exists() || file.isDirectory()) throw new FileNotFoundException();

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] buffer = new byte[256];

        while(fileInputStream.available() > 0)
        {
            int read = fileInputStream.read(buffer);

            messageDigest.update(buffer, 0, read);
        }

        fileInputStream.close();

        return messageDigest.digest();
    }

    /**
     * Digests multiple byte arrays and returns the final SHA-256 hash
     * @param values Byte arrays to be digested
     * @return Final SHA-256 hash
     * @throws NoSuchAlgorithmException If SHA-256 wasn't found on the system
     * **/
    public static byte[] digestMultiple(byte[] ...values) throws NoSuchAlgorithmException
    {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        for(byte[] value : values) messageDigest.update(value);

        return messageDigest.digest();
    }

    /**
     * Calculates the merkle root of all files in rootDir
     * @param rootDir The root directory for the merkle root calculation
     * @return The SHA-256 merkle root hash
     * @throws IOException If any file operation fails
     * @throws NoSuchAlgorithmException If SHA-256 wasn't found on the system
     * **/
    public static byte[] merkleRoot(File rootDir) throws IOException, NoSuchAlgorithmException
    {
        return merkleRoot(rootDir, (File pathname) -> true); // Accept all files
    }

    /**
     * Calculates the merkle root of all files in rootDir which pass the fileFilter
     * @param rootDir The root directory for the merkle root calculation
     * @param fileFilter Only files which pass the filtration will be added to the merkle root
     * @throws IOException If any file operation fails
     * @throws NoSuchAlgorithmException If SHA-256 wasn't found on the system
     * **/
    public static byte[] merkleRoot(File rootDir, FileFilter fileFilter) throws IOException, NoSuchAlgorithmException
    {
        if(!rootDir.exists()) throw new FileNotFoundException();

        if(rootDir.isFile()) return digest(rootDir);

        File[] files = rootDir.listFiles(fileFilter);

        if(files.length == 0) return null;
        if(files.length == 1) return merkleRoot(files[0]);

        Arrays.sort(files); // Make sure we're always reading the files in alphabetical order to get a consistent hash

        byte[][] hashes = new byte[files.length + 1][32]; // leave 1 additional empty element in case the number of entries is odd

        Set<Integer> emptyDirCorrection = new HashSet<>();

        // Calculate the hash for each file & directory
        for(int i = 0; i < files.length; i++)
        {
            byte[] currentHash;

            if(files[i].isDirectory())
            {
                currentHash = merkleRoot(files[i], fileFilter);
                if(currentHash == null)
                {
                    emptyDirCorrection.add(i);
                    continue;
                }
            }
            else
            {
                currentHash = digest(files[i]);
            }

            hashes[i] = currentHash;
        }

        int previousHashes = files.length - emptyDirCorrection.size();
        int nextHashes;

        byte[][] initialHashes;

        if(!emptyDirCorrection.isEmpty())
        {
            initialHashes = new byte[previousHashes + 1][32];

            int i = 0;

            for(byte[] hash : hashes)
            {
                if(Arrays.equals(hash, EMPTY_HASH)) continue;
                initialHashes[i] = hash;
                i++;
            }
        }
        else
        {
            initialHashes = hashes;
        }

        byte[][] newHashes = null;

        // Combine the hashes until we get to the merkle root
        while(previousHashes > 1)
        {
            nextHashes = (int) Math.ceil(previousHashes / 2.0);

            newHashes = new byte[nextHashes + 1][32];

            for(int i = 0; i < nextHashes; i++)
            {
                // If we're dealing with an odd number of hashes we need to duplicate the last one i.e. hash it with itself
                if(Arrays.equals(initialHashes[i * 2 + 1], EMPTY_HASH)) initialHashes[i * 2 + 1] = initialHashes[i];

                newHashes[i] = digestMultiple(initialHashes[i * 2], initialHashes[i * 2 + 1]);
            }

            previousHashes = nextHashes;
            initialHashes = newHashes;
        }

        return newHashes[0];
    }

}
