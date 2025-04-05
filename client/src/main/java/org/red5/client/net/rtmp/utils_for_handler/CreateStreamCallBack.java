package org.red5.client.net.rtmp.utils_for_handler;

import org.red5.client.net.rtmp.BaseRTMPClientHandler;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.stream.consumer.ConnectionConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CreateStreamCallBack implements IPendingServiceCallback {

    private static final Logger log = LoggerFactory.getLogger(CreateStreamCallBack.class);
    private IPendingServiceCallback wrapped;
    private BaseRTMPClientHandler baseRTMPClientHandler;

    public CreateStreamCallBack(IPendingServiceCallback wrapped, BaseRTMPClientHandler baseRTMPClientHandler) {
        log.debug("CreateStreamCallBack {}", wrapped.getClass().getName());
        this.wrapped = wrapped;
        this.baseRTMPClientHandler = baseRTMPClientHandler;
    }

    @Override
    public void resultReceived(IPendingServiceCall call) {
        // get the result as base object
        Object callResult = call.getResult();
        if (callResult != null) {
            // we expect a number consisting of the stream id, but we'll check for an object map as well
            int streamId = -1;
            if (callResult instanceof Number) {
                streamId = ((Number) callResult).intValue();
            } else if (callResult instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) callResult;
                // XXX(paul) log out the map contents
                log.warn("CreateStreamCallBack resultReceived - map: {}", map);
                if (map.containsKey("streamId")) {
                    Object tmpStreamId = map.get("streamId");
                    if (tmpStreamId instanceof Number) {
                        streamId = ((Number) tmpStreamId).intValue();
                    } else {
                        log.warn("CreateStreamCallBack resultReceived - stream id is not a number: {}", tmpStreamId);
                    }
                }
            }
            log.debug("CreateStreamCallBack resultReceived - stream id: {} call: {} connection: {}", streamId, call, baseRTMPClientHandler.getConnection() );
            if (baseRTMPClientHandler.getConnection() != null && streamId != -1) {
                log.debug("Setting new net stream");
                NetStream stream = new NetStream(baseRTMPClientHandler.getStreamEventDispatcher());
                stream.setConnection(baseRTMPClientHandler.getConnection() );
                stream.setStreamId(streamId);
                baseRTMPClientHandler.getConnection() .addClientStream(stream);
                NetStreamPrivateData streamData = new NetStreamPrivateData(streamId, baseRTMPClientHandler);
                streamData.outputStream = baseRTMPClientHandler.getConnection() .createOutputStream(streamId);
                streamData.connConsumer = new ConnectionConsumer(baseRTMPClientHandler.getConnection() , streamData.outputStream.getVideo(), streamData.outputStream.getAudio(), streamData.outputStream.getData());
                baseRTMPClientHandler.getStreamDataList().add(streamData);
                log.debug("streamDataList: {}",  baseRTMPClientHandler.getStreamDataList());
            }
            wrapped.resultReceived(call);
        } else {
            log.warn("CreateStreamCallBack resultReceived - call result is null");
        }
    }
}