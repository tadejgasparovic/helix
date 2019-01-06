package helix.toolkit.network.decoders;

import java.io.IOException;
import java.io.InputStream;

public class IdentityDecoderStream extends InputStream
{
    /**
     * READ_UNTIL_EOS can be passed to the constructor as contentLength to read the stream until the end
     * **/
    public static final long READ_UNTIL_EOS = -1;

    private InputStream inputStream;
    private long contentLength;
    private long read;

    /**
     * Most basic decoder stream. Reads data from the stream until it reaches the end specified by the content length.
     * @param inputStream InputStream to read
     * @param contentLength Number of bytes to read
     * **/
    public IdentityDecoderStream(InputStream inputStream, long contentLength)
    {
        this.inputStream = inputStream;
        this.contentLength = contentLength;
        this.read = 0;
    }

    @Override
    public int read() throws IOException
    {
        if(contentLength != READ_UNTIL_EOS && read >= contentLength) return -1;

        read++;

        return inputStream.read();
    }
}
