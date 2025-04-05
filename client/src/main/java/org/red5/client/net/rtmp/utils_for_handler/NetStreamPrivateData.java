package org.red5.client.net.rtmp.utils_for_handler;

import org.red5.client.net.rtmp.BaseRTMPClientHandler;
import org.red5.client.net.rtmp.INetStreamEventHandler;
import org.red5.server.stream.OutputStream;
import org.red5.server.stream.consumer.ConnectionConsumer;

public final class NetStreamPrivateData {

    public volatile INetStreamEventHandler handler;

    public volatile OutputStream outputStream;

    public volatile ConnectionConsumer connConsumer;

    private final int streamId;

    private BaseRTMPClientHandler baseRTMPClientHandler;

    NetStreamPrivateData(int streamId, BaseRTMPClientHandler baseRTMPClientHandler) {
        this.streamId = streamId;
        this.baseRTMPClientHandler = baseRTMPClientHandler;
        if ( baseRTMPClientHandler.getStreamEventHandler() != null) {
            handler = baseRTMPClientHandler.getStreamEventHandler();
        }
    }

    public int getStreamId() {
        return streamId;
    }

    @Override
    public int hashCode() {
        return streamId;
    }

}
