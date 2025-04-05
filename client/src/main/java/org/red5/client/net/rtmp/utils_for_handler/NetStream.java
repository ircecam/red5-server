package org.red5.client.net.rtmp.utils_for_handler;

import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.stream.AbstractClientStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetStream extends AbstractClientStream implements IEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NetStream.class);

    private IEventDispatcher dispatcher;

    public NetStream(IEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void close() {
        log.debug("NetStream close");
    }

    @Override
    public void start() {
        log.debug("NetStream start");
    }

    @Override
    public void stop() {
        log.debug("NetStream stop");
    }

    @Override
    public void dispatchEvent(IEvent event) {
        log.debug("NetStream dispatchEvent: {}", event);
        if (dispatcher != null) {
            dispatcher.dispatchEvent(event);
        }
    }
}
