package org.red5.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractVideoCodec extends AbstractVideo {

    private static final Logger log = LoggerFactory.getLogger(AbstractVideoCodec.class);

    protected abstract String getCodecName();

    protected abstract boolean isKeyFrame(byte[] data, int offset);

    protected abstract boolean isCorrectCodec(byte frameType);

    protected abstract byte[] getKeyFramePrefix();

    protected abstract byte[] getFramePrefix();

    @Override
    public String getName() {
        return getCodecName();
    }

    @Override
    public boolean canDropFrames() {
        return true;
    }

    @Override
    public boolean addData(IoBuffer data, int timestamp) {
        return addData(data, timestamp, true);
    }

    @Override
    public boolean addData(IoBuffer data, int timestamp, boolean amf) {
        log.trace("addData timestamp: {} remaining: {} amf? {}", timestamp, data.remaining(), amf);
        if (data.hasRemaining()) {
            int start = data.position();
            if (!amf) {
                int remaining = data.remaining();
                if (remaining > 7) {
                    byte[] peek = new byte[8];
                    data.get(peek);
                    boolean isKey = isKeyFrame(peek, 0);
                    data.position(start);
                    IoBuffer slice = data.getSlice(start, remaining);
                    data.expand(remaining + 2);
                    if (isKey) {
                        data.put(getKeyFramePrefix());
                    } else {
                        data.put(getFramePrefix());
                    }
                    data.put(slice);
                    data.flip();
                    start = data.position();
                } else {
                    log.warn("Remaining {} content was less than expected: {}", getCodecName(), remaining);
                }
                data.position(start);
            }
            byte frameType = data.get();
            byte subFrameType = data.get();
            if (isCorrectCodec(frameType)) {
                if ((frameType & 0xf0) == FLV_FRAME_KEY) {
                    if (log.isDebugEnabled()) {
                        log.debug("Keyframe - {} type: {}", getCodecName(), subFrameType);
                    }
                    data.rewind();
                    switch (subFrameType) {
                        case 1:
                            if (timestamp != keyframeTimestamp) {
                                keyframeTimestamp = timestamp;
                                keyframes.clear();
                            }
                            keyframes.add(new FrameData(data));
                            break;
                        case 0:
                            break;
                    }
                }
            } else {
                log.debug("Non-{} data, rejecting", getCodecName());
                data.position(start);
                return false;
            }
            data.position(start);
        }
        return true;
    }
}
