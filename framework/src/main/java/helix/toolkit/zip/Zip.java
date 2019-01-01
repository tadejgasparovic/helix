package helix.toolkit.zip;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zip
{

    /**
     * Extracts the contents of a zip file
     * @param source The zip file to extract
     * @param target The directory to extract the zip file to
     * @throws IOException If the extraction fails
     * **/
    public static void extract(File source, File target) throws IOException
    {
        if(source == null || !source.exists()) throw new FileNotFoundException("Source zip");
        if(target == null || !target.isDirectory() || (!target.exists() && !target.mkdirs())) throw new FileNotFoundException("Target dir");

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(source));

        ZipEntry zipEntry = zipInputStream.getNextEntry();

        while(zipEntry != null)
        {
            String targetPath = target.getAbsolutePath() + File.separator + zipEntry.getName();

            if(zipEntry.isDirectory())
            {
                File targetDir = new File(targetPath);
                targetDir.mkdir();
            }
            else
            {
                Path targetFile = Paths.get(targetPath);

                Files.copy(zipInputStream, targetFile);
            }

            zipInputStream.closeEntry();
            zipEntry = zipInputStream.getNextEntry();
        }

        zipInputStream.close();
    }

    /**
     * Compresses files into a zip
     * @param target The target zip file
     * @param rootDir Local directory used as the root for the zip directory structure. All `files` need to be children of this path
     * @param files The files to be compressed
     * @throws IOException If compression fails
     * **/
    public static void compress(File target, File rootDir, File ...files) throws IOException
    {
        if(target == null || rootDir == null || files == null || files.length == 0) throw new ZipException("Can't call Zip.compress() with null arguments");

        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(target));

        for(File f : files)
        {
            Set<String> entryNames = generateEntryList(f, rootDir);

            for(String entryName : entryNames)
            {
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                File zipEntryFile = new File(rootDir.getAbsolutePath() + File.separator + entryName);

                Files.copy(zipEntryFile.toPath(), zipOutputStream);

                zipOutputStream.closeEntry();
            }
        }

        zipOutputStream.close();
    }

    /**
     * Recursively generates a list of all files and directories inside rootDir
     * @param file The starting point
     * @param rootDir The root directory of the starting point
     * @return A set of all zip entry names
     * @throws FileNotFoundException If any of the parameters are null (rootDir can be null if file is a directory)
     * **/
    private static Set<String> generateEntryList(File file, File rootDir) throws FileNotFoundException
    {
        if(file == null) throw new FileNotFoundException();
        if(rootDir == null && file.isDirectory()) rootDir = file;
        if(rootDir == null) throw new NullPointerException();

        Set<String> fileSet = new HashSet<>();

        if(file.isFile())
        {
            fileSet.add(zipEntryFileName(file, rootDir));
            return fileSet;
        }

        File[] files = file.listFiles();

        if(files == null) return fileSet;

        for(File f : files)
        {
            if(f.isFile()) fileSet.add(zipEntryFileName(f, rootDir));
            if(f.isDirectory()) fileSet.addAll(generateEntryList(f, rootDir));
        }

        return fileSet;
    }

    /**
     * Generates the zip entry filename
     * @param file The file to generate the zip entry name for
     * @param root The root directory of the file (needed to determine directory structure of zip)
     * @return The zip entry name
     * **/
    private static String zipEntryFileName(File file, File root)
    {
        if(file == null || root == null) throw new NullPointerException();
        String absolutePath = file.getAbsolutePath();
        return absolutePath.substring(root.getAbsolutePath().length() + 1, absolutePath.length());
    }

}
