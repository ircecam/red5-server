package org.red5.io.utils;

import java.io.IOException;

public interface DataEncoder {
    byte[] encode(byte[] data) throws IOException;
}