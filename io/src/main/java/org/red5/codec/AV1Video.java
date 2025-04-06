/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2022 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Red5 video codec for the AV1 video format.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AV1Video extends AbstractVideoCodec {

    private static Logger log = LoggerFactory.getLogger(AV1Video.class);

    static final String CODEC_NAME = "AV1";
    public static final byte[] AV1_KEYFRAME_PREFIX = new byte[] { 0x0a, 0x01 };
    public static final byte[] AV1_FRAME_PREFIX = new byte[] { 0x2a, 0x01 };

    @Override
    protected String getCodecName() {
        return CODEC_NAME;
    }

    @Override
    protected boolean isKeyFrame(byte[] data, int offset) {
        return true; // Implement this based on AV1 keyframe detection logic
    }

    @Override
    protected boolean isCorrectCodec(byte frameType) {
        return (frameType & 0x0f) == VideoCodec.AV1.getId();
    }

    @Override
    protected byte[] getKeyFramePrefix() {
        return AV1_KEYFRAME_PREFIX;
    }

    @Override
    protected byte[] getFramePrefix() {
        return AV1_FRAME_PREFIX;
    }
}
