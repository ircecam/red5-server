package org.red5.client.net.rtmp.utils_for_handler;

import org.red5.client.net.rtmp.BaseRTMPClientHandler;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DeleteStreamCallBack implements IPendingServiceCallback {

    private static final Logger log = LoggerFactory.getLogger(DeleteStreamCallBack.class);

    private IPendingServiceCallback wrapped;

    private BaseRTMPClientHandler baseRTMPClientHandler;

    public DeleteStreamCallBack(IPendingServiceCallback wrapped, BaseRTMPClientHandler baseRTMPClientHandler) {
        log.debug("DeleteStreamCallBack {}", wrapped.getClass().getName());
        this.wrapped = wrapped;
        this.baseRTMPClientHandler = baseRTMPClientHandler;
    }

    @Override
    public void resultReceived(IPendingServiceCall call) {
        // get the result as base object
        Object callResult = call.getResult();
        if (callResult != null) {
            // we expect a number consisting of the stream id, but we'll check for an object map as well
            final Number streamId = (Number) (callResult instanceof Number ? callResult : (callResult instanceof Map ? ((Map<?, ?>) callResult).get("streamId") : 1.0));
            log.debug("DeleteStreamCallBack resultReceived - stream id: {} call: {} connection: {}", streamId, call, baseRTMPClientHandler.getConnection());
            if (baseRTMPClientHandler.getConnection() != null) {
                log.debug("Deleting net stream");
                baseRTMPClientHandler.getConnection().removeClientStream(streamId);
                // send a delete notify?
                final int sid = streamId.intValue();
                NetStreamPrivateData streamData = baseRTMPClientHandler.getStreamDataList().stream().filter(s -> s.getStreamId() == sid).findFirst().orElse(null);
                if (streamData != null) {
                    baseRTMPClientHandler.getStreamDataList().remove(streamData);
                } else {
                    log.warn("Stream data not found for stream id: {}", streamId);
                }
            }
            wrapped.resultReceived(call);
        } else {
            log.warn("DeleteStreamCallBack resultReceived - call result is null");
        }
    }
}
