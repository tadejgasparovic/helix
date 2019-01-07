package helix.toolkit.network.decoders;

import helix.toolkit.streams.StreamUtils;

import java.io.IOException;
import java.io.InputStream;

public class ChunkedDecoderStream extends InputStream
{
    private InputStream inputStream;
    private boolean connectionCloseRequested;

    private int currentChunkLength;
    private int lastChunkByte;

    /**
     * Wraps an InputStream into a ChunkedDecoderStream
     * @param inputStream InputStream to be wrapped and decoded as "chunked"
     * @param connectionCloseRequested Did the server close the connection?
     * **/
    public ChunkedDecoderStream(InputStream inputStream, boolean connectionCloseRequested)
    {
        this.inputStream = inputStream;
        this.connectionCloseRequested = connectionCloseRequested;
        this.currentChunkLength = 0;
        this.lastChunkByte = 0;
    }

    /**
     * Reads the next byte of the chunked data
     * @return Next byte
     * @throws IOException If the read fails
     * **/
    @Override
    public int read() throws IOException
    {
        if(!connectionCloseRequested && lastChunkByte == currentChunkLength)
        {
            if(lastChunkByte > 0)
            {
                // Skip CRLF at the end of the last chunk
                for(int i = 0; i < 2; i++) inputStream.read();
            }

            // Expecting the next line to contain the chunk length in hex
            String chunkLengthLine = StreamUtils.readLine(inputStream);

            currentChunkLength = 0;

            for(int i = 0; i < chunkLengthLine.length(); i++)
            {
                currentChunkLength |= Character.digit(chunkLengthLine.charAt(i), 16) << (chunkLengthLine.length() - 1 - i) * 4;
            }

            if(currentChunkLength == 0) return -1; // We've reached the end!

            lastChunkByte = 0;
        }

        lastChunkByte++;

        return inputStream.read();
    }
}
