package org.red5.client.net.rtmp.utils_for_handler;

import org.red5.client.net.rtmp.BaseRTMPClientHandler;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribeStreamCallBack implements IPendingServiceCallback {

    private static final Logger log = LoggerFactory.getLogger(DeleteStreamCallBack.class);

    private IPendingServiceCallback wrapped;

    private BaseRTMPClientHandler handler;

    public SubscribeStreamCallBack(IPendingServiceCallback wrapped, BaseRTMPClientHandler handler) {
        log.debug("SubscribeStreamCallBack {}", wrapped.getClass().getName());
        this.wrapped = wrapped;
        this.handler = handler;
    }

    @Override
    public void resultReceived(IPendingServiceCall call) {
        log.debug("resultReceived", call);
        if (call.getResult() instanceof ObjectMap<?, ?>) {
            ObjectMap<?, ?> map = (ObjectMap<?, ?>) call.getResult();
            if (map.containsKey("code")) {
                String code = (String) map.get("code");
                log.debug("Code: {}", code);
                if (StatusCodes.NS_PLAY_START.equals(code)) {
                    handler.setSubscribed(true);
                }
            }
        }
        wrapped.resultReceived(call);
    }
}