package org.red5.io.utils;

public final class ValidationUtils {

    public boolean isValidUint8(short value) {
        return (value & 0xFF) == value;
    }

    public boolean isValidUint8(int value) {
        return (value & 0xFF) == value;
    }

    public boolean isValidUint8(long value) {
        return (value & 0xFFL) == value;
    }

    public boolean isValidUint16(int value) {
        return (value & 0xFFFF) == value;
    }

    public boolean isValidUint16(long value) {
        return (value & 0xFFFFL) == value;
    }

    public boolean isValidUint24(int value) {
        return (value & 0xFFFFFF) == value;
    }

    public boolean isValidUint24(long value) {
        return (value & 0xFFFFFFL) == value;
    }

    public boolean isValidUint32(long value) {
        return (value & 0xFFFFFFFFL) == value;
    }

    public boolean isValidUint48(long value) {
        return (value & 0xFFFFFFFFFFFFL) == value;
    }
}