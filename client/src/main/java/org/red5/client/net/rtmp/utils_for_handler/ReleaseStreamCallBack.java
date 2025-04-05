package org.red5.client.net.rtmp.utils_for_handler;

import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReleaseStreamCallBack implements IPendingServiceCallback {

    private static final Logger log = LoggerFactory.getLogger(ReleaseStreamCallBack.class);

    private IPendingServiceCallback wrapped;

    public ReleaseStreamCallBack(IPendingServiceCallback wrapped) {
        log.debug("ReleaseStreamCallBack {}", wrapped.getClass().getName());
        this.wrapped = wrapped;
    }

    @Override
    public void resultReceived(IPendingServiceCall call) {
        wrapped.resultReceived(call);
    }
}
