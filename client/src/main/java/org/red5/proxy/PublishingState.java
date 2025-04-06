package org.red5.proxy;

import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.net.rtmp.event.Notify;

public class PublishingState implements StreamingState {

    @Override
    public void start(StreamingProxy streamingProxy, String publishName, String publishMode, Object[] params) {
        throw new IllegalStateException("Already in the publishing process.");
    }

    @Override
    public void stop(StreamingProxy streamingProxy) {
        streamingProxy.getRtmpClient().disconnect();
        streamingProxy.setState(new StoppedState());
    }

    @Override
    public void createStream(StreamingProxy streamingProxy) {
        streamingProxy.getRtmpClient().createStream(streamingProxy);
    }

    @Override
    public void onStreamEvent(StreamingProxy streamingProxy, Notify notify) {

    }

    @Override
    public void resultReceived(StreamingProxy streamingProxy, IPendingServiceCall call) {
        if ("createStream".equals(call.getServiceMethodName())) {
            Object result = call.getResult();
            if (result instanceof Number) {
                streamingProxy.setStreamId((Number) result);
                streamingProxy.getRtmpClient().publish(streamingProxy.getStreamId(), streamingProxy.getPublishName(), streamingProxy.getPublishMode(), streamingProxy);
            } else {
                streamingProxy.setState(new StoppedState());
            }
        }
    }
}