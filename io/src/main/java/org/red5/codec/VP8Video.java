/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Red5 video codec for the VP8 video format.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class VP8Video extends AbstractVideoCodec {

    private static Logger log = LoggerFactory.getLogger(VP8Video.class);

    static final String CODEC_NAME = "VP8";
    public static final byte[] VP8_KEYFRAME_PREFIX = new byte[] { 0x18, 0x01 };
    public static final byte[] VP8_FRAME_PREFIX = new byte[] { 0x28, 0x01 };

    private static final byte S_BIT = (byte) 0x10;
    private static final byte X_BIT = (byte) 0x80;
    private static final byte I_BIT = (byte) 0x80;
    private static final byte M_BIT = (byte) 0x80;
    private static final byte L_BIT = (byte) 0x40;
    private static final byte T_BIT = (byte) 0x20;
    private static final byte K_BIT = (byte) 0x10;

    @Override
    protected String getCodecName() {
        return CODEC_NAME;
    }

    @Override
    protected boolean isKeyFrame(byte[] data, int offset) {
        return (data[offset] & S_BIT) == 0;
    }

    @Override
    protected boolean isCorrectCodec(byte frameType) {
        return (frameType & 0x0f) == VideoCodec.VP8.getId();
    }

    @Override
    protected byte[] getKeyFramePrefix() {
        return VP8_KEYFRAME_PREFIX;
    }

    @Override
    protected byte[] getFramePrefix() {
        return VP8_FRAME_PREFIX;
    }

    private static int getDescriptorSize(byte[] input, int offset, int length) {
        if ((input[offset] & X_BIT) == 0) {
            return 1;
        }
        int size = 2;
        if ((input[offset + 1] & I_BIT) != 0) {
            size++;
            if ((input[offset + 2] & M_BIT) != 0) {
                size++;
            }
        }
        if ((input[offset + 1] & L_BIT) != 0) {
            size++;
        }
        if ((input[offset + 1] & (T_BIT | K_BIT)) != 0) {
            size++;
        }
        return size;
    }
}
