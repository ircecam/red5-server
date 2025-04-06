package org.red5.proxy;

import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.net.rtmp.event.Notify;

public class UninitializedState implements StreamingState {

    @Override
    public void start(StreamingProxy streamingProxy, String publishName, String publishMode, Object[] params) {
        streamingProxy.init(ClientType.RTMP);
        streamingProxy.setState(new ConnectingState());
        streamingProxy.getState().start(streamingProxy, publishName, publishMode, params);
    }

    @Override
    public void stop(StreamingProxy streamingProxy) {
        throw new IllegalStateException("Cannot stop. StreamingProxy is not initialized.");
    }

    @Override
    public void createStream(StreamingProxy streamingProxy) {
        throw new IllegalStateException("Cannot create stream. StreamingProxy is not initialized.");
    }

    @Override
    public void onStreamEvent(StreamingProxy streamingProxy, Notify notify) {
        throw new IllegalStateException("Cannot handle stream event. StreamingProxy is not initialized.");
    }

    @Override
    public void resultReceived(StreamingProxy streamingProxy, IPendingServiceCall call) {
        throw new IllegalStateException("Cannot handle result. StreamingProxy is not initialized.");
    }
}