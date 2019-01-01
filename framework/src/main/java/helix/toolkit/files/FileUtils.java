package helix.toolkit.files;

import java.io.File;

public class FileUtils
{

    /**
     * Deletes a directory by recursively deleting subdirectories and their contents
     * @param directory The directory to be deleted
     * **/
    public static void deleteDirectoryRecursively(File directory)
    {
        if(directory.isFile()) return;

        File[] files = directory.listFiles();

        if(files == null)
        {
            directory.delete();
            return;
        }

        for(File file : files)
        {
            if(file.isFile()) file.delete();
            if(file.isDirectory())
            {
                deleteDirectoryRecursively(file);
                file.delete();
            }
        }
    }
}
