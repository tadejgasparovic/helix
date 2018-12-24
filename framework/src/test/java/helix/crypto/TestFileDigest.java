package helix.crypto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TestFileDigest
{
    /**
     * Generates all necessary files for testing
     * **/
    @BeforeAll
    public static void createTestFiles() throws IOException
    {
        new File("./test-resources/").mkdir();
        Files.write(Paths.get("./test-resources/test"), "Just a test file".getBytes());
        new File("./test-resources/testdir").mkdir();
        Files.write(Paths.get("./test-resources/testdir/test"), "Just another test file".getBytes());
    }

    /**
     * Cleans up all test files
     * **/
    @AfterAll
    public static void destroyTestFiles() throws IOException
    {
        Files.deleteIfExists(Paths.get("./test-resources/test"));
        Files.deleteIfExists(Paths.get("./test-resources/testdir/test"));
        Files.deleteIfExists(Paths.get("./test-resources/testdir"));
        Files.deleteIfExists(Paths.get("./test-resources"));
    }

    @Test
    public void doesDigestFileWithSHA256() throws IOException, NoSuchAlgorithmException
    {
        byte[] hash = FileDigest.digest(new File("./test"));
        assertArrayEquals(hash, new byte[]{
                (byte)0x2d,
                (byte)0x36,
                (byte)0xa5,
                (byte)0xfe,
                (byte)0x6c,
                (byte)0xc1,
                (byte)0x2d,
                (byte)0xcd,
                (byte)0x4c,
                (byte)0x9b,
                (byte)0x2a,
                (byte)0xe8,
                (byte)0x46,
                (byte)0xa4,
                (byte)0xb1,
                (byte)0xaa,
                (byte)0x1e,
                (byte)0x1d,
                (byte)0x61,
                (byte)0xe0,
                (byte)0x6a,
                (byte)0x76,
                (byte)0xa1,
                (byte)0x26,
                (byte)0x2a,
                (byte)0x2d,
                (byte)0x74,
                (byte)0xfa,
                (byte)0x6e,
                (byte)0x84,
                (byte)0xe2,
                (byte)0x62
        });
    }

    @Test
    public void doesCalculateCorrectSHA256MerkleRoot() throws IOException, NoSuchAlgorithmException
    {
        byte[] hash = FileDigest.merkleRoot(new File("./test-resources/"));
        assertArrayEquals(hash, new byte[]{
                //
        });
    }
}
