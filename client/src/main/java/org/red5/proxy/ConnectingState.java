package org.red5.proxy;

import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.net.rtmp.event.Notify;

public class ConnectingState implements StreamingState {

    @Override
    public void start(StreamingProxy streamingProxy, String publishName, String publishMode, Object[] params) {
    }

    @Override
    public void stop(StreamingProxy streamingProxy) {
        streamingProxy.getRtmpClient().disconnect();
        streamingProxy.setState(new StoppedState());
    }

    @Override
    public void createStream(StreamingProxy streamingProxy) {
        throw new IllegalStateException("Cannot create stream while connecting.");
    }

    @Override
    public void onStreamEvent(StreamingProxy streamingProxy, Notify notify) {
    }

    @Override
    public void resultReceived(StreamingProxy streamingProxy, IPendingServiceCall call) {
        if ("connect".equals(call.getServiceMethodName())) {
            streamingProxy.setState(new PublishingState());
        }
    }
}
