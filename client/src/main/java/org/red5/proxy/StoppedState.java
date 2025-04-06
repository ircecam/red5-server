package org.red5.proxy;


import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.net.rtmp.event.Notify;

public class StoppedState implements StreamingState {

    @Override
    public void start(StreamingProxy streamingProxy, String publishName, String publishMode, Object[] params) {
        streamingProxy.setState(new ConnectingState());
        streamingProxy.getState().start(streamingProxy, publishName, publishMode, params);
    }

    @Override
    public void stop(StreamingProxy streamingProxy) {
    }

    @Override
    public void createStream(StreamingProxy streamingProxy) {
        throw new IllegalStateException("Cannot create stream. StreamingProxy is stopped.");
    }

    @Override
    public void onStreamEvent(StreamingProxy streamingProxy, Notify notify) {

    }

    @Override
    public void resultReceived(StreamingProxy streamingProxy, IPendingServiceCall call) {

    }
}
