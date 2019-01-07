package helix.toolkit.streams;

import java.io.IOException;
import java.io.InputStream;

public class StreamUtils
{
    /**
     * Reads a line from the input stream using \r\n as the delimiter
     * @param inputStream Input stream to read from
     * @return Line read from the input stream
     * @throws IOException If the read fails
     * **/
    public static String readLine(InputStream inputStream) throws IOException
    {
        return readLine(inputStream, "\r\n");
    }

    /**
     * Reads a line from the input stream
     * @param inputStream Input stream to read from
     * @param delim Delimiter string separating lines
     * @return Line read from the input stream
     * @throws IOException If the read fails
     * **/
    public static String readLine(InputStream inputStream, String delim) throws IOException
    {
        StringBuilder stringBuilder = new StringBuilder();

        int read;

        int delimIdx = 0;

        StringBuilder partialDelimBuffer = new StringBuilder();

        while((read = inputStream.read()) > -1 && delimIdx < delim.length() - 1)
        {
            if(read == delim.charAt(delimIdx))
            {
                delimIdx++;
                partialDelimBuffer.append((char) read);
            }
            else
            {
                if(partialDelimBuffer.length() > 0)
                {
                    stringBuilder.append(partialDelimBuffer);
                    partialDelimBuffer = new StringBuilder();
                }
                delimIdx = 0;
                stringBuilder.append((char) read);
            }
        }

        if(read < 0 && stringBuilder.length() < 1) return null;

        return stringBuilder.toString();
    }
}
