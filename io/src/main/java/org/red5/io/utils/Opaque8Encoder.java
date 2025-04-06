package org.red5.io.utils;

import java.io.IOException;

public class Opaque8Encoder implements DataEncoder {

    @Override
    public byte[] encode(byte[] data) throws IOException {
        byte[] result = new byte[1 + data.length];
        result[0] = (byte) data.length;
        System.arraycopy(data, 0, result, 1, data.length);
        return result;
    }
}
