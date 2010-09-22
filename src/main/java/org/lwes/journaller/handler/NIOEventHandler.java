package org.lwes.journaller.handler;
/**
 * @author fmaritato
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lwes.journaller.DeJournaller;
import org.lwes.journaller.util.EventHandlerUtil;
import org.lwes.listener.DatagramQueueElement;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class NIOEventHandler extends AbstractFileEventHandler {

    private static transient Log log = LogFactory.getLog(NIOEventHandler.class);

    private FileChannel channel = null;
    private FileOutputStream out = null;

    private ByteBuffer headerBuffer = ByteBuffer.allocateDirect(DeJournaller.MAX_HEADER_SIZE);
    private ByteBuffer bodyBuffer = ByteBuffer.allocateDirect(DeJournaller.MAX_BODY_SIZE);

    public NIOEventHandler() {
    }

    public NIOEventHandler(String filePattern) throws IOException {
        setFilenamePattern(filePattern);
        generateFilename();
        createOutputStream();

        headerBuffer.clear();
        bodyBuffer.clear();
    }

    public boolean rotate() throws IOException {
        channel.close();
        out.close();
        generateFilename();
        createOutputStream();
        return true;
    }

    public void createOutputStream() throws IOException {
        out = new FileOutputStream(getFilename(), true);
        if (log.isDebugEnabled()) {
            log.debug("using file: " + getFilename());
        }
        channel = out.getChannel();
    }

    public void destroy() {
        try {
            channel.close();
            out.close();
        }
        catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void handleEvent(DatagramQueueElement element) throws IOException {
        DatagramPacket packet = element.getPacket();

        EventHandlerUtil.writeHeader(packet.getLength(),
                                     element.getTimestamp(),
                                     packet.getAddress(),
                                     packet.getPort(),
                                     getSiteId(),
                                     headerBuffer);

        headerBuffer.flip();
        channel.write(headerBuffer);
        bodyBuffer.put(packet.getData());
        bodyBuffer.flip();
        channel.write(bodyBuffer);
        out.flush();
        headerBuffer.clear();
        bodyBuffer.clear();
    }

    public String getFileExtension() {
        return ".log";
    }

    public void closeOutputStream() throws IOException {
        out.flush();
        out.close();
    }

    public ObjectName getObjectName() throws MalformedObjectNameException {
        return new ObjectName("org.lwes:name=NIOEventHandler");
    }
}
