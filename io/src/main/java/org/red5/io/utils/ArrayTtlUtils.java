package org.red5.io.utils;

import java.util.Arrays;

public final class ArrayTtlUtils {
    public static final byte[] EMPTY_BYTES = new byte[0];
    public static final short[] EMPTY_SHORTS = new short[0];
    public static final int[] EMPTY_INTS = new int[0];

    public static byte[] clone(byte[] data) {
        return data == null ? null : data.length == 0 ? EMPTY_BYTES : data.clone();
    }

    public static short[] clone(short[] data) {
        return data == null ? null : data.length == 0 ? EMPTY_SHORTS : data.clone();
    }

    public static boolean contains(short[] array, short value) {
        for (short v : array) {
            if (v == value) return true;
        }
        return false;
    }

    public static boolean contains(int[] array, int value) {
        for (int v : array) {
            if (v == value) return true;
        }
        return false;
    }

    public static short[] truncate(short[] array, int newLength) {
        if (array.length <= newLength) return array;
        return Arrays.copyOf(array, newLength);
    }

    public static byte[] copyOfRangeExact(byte[] original, int from, int to) {
        byte[] copy = new byte[to - from];
        System.arraycopy(original, from, copy, 0, copy.length);
        return copy;
    }
}