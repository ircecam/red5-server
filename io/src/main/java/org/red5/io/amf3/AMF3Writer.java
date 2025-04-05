package org.red5.io.amf3;

import org.apache.mina.core.buffer.IoBuffer;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;


public class AMF3Writer {
    protected final IoBuffer buf;
    protected final ConcurrentMap<String, Integer> stringReferences;
    protected final ConcurrentMap<Object, Integer> objectReferences;
    protected int amf3_mode;

    private static final long MIN_29BIT_VALUE = -268435456L;
    private static final long MAX_29BIT_VALUE = 268435455L;
    private static final long SINGLE_BYTE_MAX = 128L;
    private static final long DOUBLE_BYTE_MAX = 16384L;
    private static final long TRIPLE_BYTE_MAX = 2097152L;
    private static final long QUAD_BYTE_MAX = 1073741824L;

    /**
     * Constructs an AMF3Writer with a provided buffer for serialization.
     *
     * @param buf the IoBuffer to write data into
     */
    public AMF3Writer(IoBuffer buf) {
        this.buf = buf;
        this.stringReferences = new ConcurrentHashMap<>(8, 0.9f, 2);
        this.objectReferences = new ConcurrentHashMap<>(8, 0.9f, 2);
        this.amf3_mode = 0;
    }

    /**
     * Enables AMF3 mode for the writer.
     */
    public void enforceAMF3() {
        this.amf3_mode++;
    }

    /**
     * Writes the AMF3 header to the buffer.
     */
    protected void writeAMF3Header() {
        if (amf3_mode == 0) {
            buf.put((byte) 0x11);
        }
    }

    /**
     * Writes a 29-bit encoded integer to the buffer.
     *
     * @param value the long integer value to encode and write
     */
    public void writeInteger(long value) {
        if (value >= MIN_29BIT_VALUE && value <= MAX_29BIT_VALUE) {
            value &= 0x1FFFFFFF;
        }

        if (value < SINGLE_BYTE_MAX) {
            buf.put((byte) value);
        } else if (value < DOUBLE_BYTE_MAX) {
            buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
            buf.put((byte) (value & 0x7F));
        } else if (value < TRIPLE_BYTE_MAX) {
            buf.put((byte) (((value >> 14) & 0x7F) | 0x80));
            buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
            buf.put((byte) (value & 0x7F));
        } else if (value < QUAD_BYTE_MAX) {
            buf.put((byte) (((value >> 22) & 0x7F) | 0x80));
            buf.put((byte) (((value >> 15) & 0x7F) | 0x80));
            buf.put((byte) (((value >> 8) & 0x7F) | 0x80));
            buf.put((byte) (value & 0xFF));
        } else {
            throw new IllegalArgumentException("Integer out of range: " + value);
        }
    }

    /**
     * Retrieves the underlying buffer used by this writer.
     *
     * @return the IoBuffer
     */
    public IoBuffer getBuffer() {
        return buf;
    }

    /**
     * Checks if an object has already been referenced.
     *
     * @param obj the object to check
     * @return true if the object is already referenced, false otherwise
     */
    public boolean hasReference(Object obj) {
        return objectReferences.containsKey(obj);
    }

    /**
     * Stores a new reference for an object.
     *
     * @param obj the object to store
     */
    public void storeReference(Object obj) {
        objectReferences.put(obj, objectReferences.size());
    }

    /**
     * Gets the reference ID for a previously stored object.
     *
     * @param obj the object to get the reference ID for
     * @return the reference ID
     */
    public int getReferenceId(Object obj) {
        return objectReferences.get(obj);
    }
}