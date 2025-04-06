/*
 * RED5 Open Source Flash Server - https://github.com/Red5/ Copyright 2006-2015 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.proxy;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.red5.client.net.rtmp.ClientExceptionHandler;
import org.red5.client.net.rtmp.INetStreamEventHandler;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.client.net.rtmpe.RTMPEClient;
import org.red5.client.net.rtmps.RTMPSClient;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamingProxy implements IPushableConsumer, IPipeConnectionListener, INetStreamEventHandler, IPendingServiceCallback {

    private static Logger log = LoggerFactory.getLogger(StreamingProxy.class);

    private StreamingState state;
    private RTMPClient rtmpClient;
    private ConcurrentLinkedQueue<IMessage> frameBuffer = new ConcurrentLinkedQueue<>();
    private Semaphore lock = new Semaphore(1, true);
    private Timer timer;

    private String host;
    private int port;
    private String app;
    private String publishName;
    private String publishMode;
    private Number streamId;

    public StreamingProxy() {
        this.state = new UninitializedState();
    }

    public void init(ClientType clientType) {
        switch (clientType) {
            case RTMPE:
                rtmpClient = new RTMPEClient();
                break;
            case RTMPS:
                rtmpClient = new RTMPSClient();
                break;
            case RTMP:
            default:
                rtmpClient = new RTMPClient();
        }
        log.debug("Initialized: {}", rtmpClient);
    }

    public void start(String publishName, String publishMode, Object[] params) {
        this.state.start(this, publishName, publishMode, params);
    }

    public void stop() {
        this.state.stop(this);
    }

    public void onStreamEvent(Notify notify) {
        this.state.onStreamEvent(this, notify);
    }

    public void resultReceived(IPendingServiceCall call) {
        this.state.resultReceived(this, call);
    }

    public void setState(StreamingState state) {
        try {
            lock.acquire();
            this.state = state;
        } catch (InterruptedException e) {
            log.warn("State transition issue.", e);
        } finally {
            lock.release();
        }
    }

    public StreamingState getState() {
        return this.state;
    }

    public RTMPClient getRtmpClient() {
        return this.rtmpClient;
    }

    public void setStreamId(Number streamId) {
        this.streamId = streamId;
    }

    public String getPublishName() {
        return publishName;
    }

    public String getPublishMode() {
        return publishMode;
    }

    public Number getStreamId() {
        return streamId;
    }

    @Override
    public void pushMessage(IPipe pipe, IMessage message) throws IOException {
        if (isPublished() && message instanceof RTMPMessage) {
            RTMPMessage rtmpMsg = (RTMPMessage) message;
            rtmpClient.publishStreamData(streamId, rtmpMsg);
        } else {
            log.trace("Adding message to buffer. Current size: {}", frameBuffer.size());
            frameBuffer.add(message);
        }
    }

    @Override
    public void onPipeConnectionEvent(PipeConnectionEvent event) {
        if (event == null) {
            log.warn("Received null PipeConnectionEvent.");
            return;
        }

        log.debug("onPipeConnectionEvent: {}", event);

        Object type = event.getType();

        if (type != null) {
            log.info("PipeConnectionEvent type: {}", type);

            switch (type.toString()) {
                case "provider_connect":
                    log.info("Provider connected.");
                    break;

                case "provider_disconnect":
                    log.info("Provider disconnected.");
                    break;

                case "consumer_connect":
                    log.info("Consumer connected.");
                    break;

                case "consumer_disconnect":
                    log.info("Consumer disconnected.");
                    break;

                default:
                    log.warn("Unknown PipeConnectionEvent type: {}", type);
                    break;
            }
        } else {
            log.warn("PipeConnectionEvent type is null. Unable to handle.");
        }
    }

    @Override
    public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
        log.debug("onOOBControlMessage: {}", oobCtrlMsg);
        if ("checkBandwidth".equals(oobCtrlMsg.getTarget())) {
            log.info("Bandwidth check requested.");
        }
    }

    public boolean isPublished() {
        return state instanceof PublishingState;
    }

    public boolean isRunning() {
        return !(state instanceof StoppedState);
    }

    public void setConnectionClosedHandler(Runnable connectionClosedHandler) {
        log.debug("setConnectionClosedHandler: {}", connectionClosedHandler);
        if (rtmpClient != null) {
            rtmpClient.setConnectionClosedHandler(connectionClosedHandler);
        } else {
            log.warn("RTMP client not initialized. Ensure init is called first.");
        }
    }

    public void setExceptionHandler(ClientExceptionHandler exceptionHandler) {
        log.debug("setExceptionHandler: {}", exceptionHandler);
        if (rtmpClient != null) {
            rtmpClient.setExceptionHandler(exceptionHandler);
        } else {
            log.warn("RTMP client not initialized. Ensure init is called first.");
        }
    }
}