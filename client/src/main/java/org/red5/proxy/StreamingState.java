package org.red5.proxy;

import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.net.rtmp.event.Notify;

public interface StreamingState {
    void start(StreamingProxy streamingProxy, String publishName, String publishMode, Object[] params);
    void stop(StreamingProxy streamingProxy);
    void createStream(StreamingProxy streamingProxy);
    void onStreamEvent(StreamingProxy streamingProxy, Notify notify);
    void resultReceived(StreamingProxy streamingProxy, IPendingServiceCall call);
}
