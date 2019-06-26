package io.quarkus.myfaces.runtime.graal;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import com.lowagie.text.pdf.PdfException;

//@TargetClass(className = "com.lowagie.text.pdf.MappedRandomAccessFile")
public final class Substitute_MappedRandomAccessFile {

    //@Alias
    private MappedByteBuffer mappedByteBuffer = null;
    //@Alias
    private FileChannel channel = null;

    //@Substitute
    private void init(FileChannel channel, FileChannel.MapMode mapMode)
            throws IOException {

        if (channel.size() > Integer.MAX_VALUE) {
            throw new PdfException(new RuntimeException("The PDF file is too large. Max 2GB. Size: " + channel.size()));
        }

        this.channel = channel;
        this.mappedByteBuffer = channel.map(mapMode, 0L, channel.size());
        //mappedByteBuffer.load();  Unsupported method java.nio.MappedByteBuffer.load0(long, long) is reachable: Native method
    }
}
