package org.red5.io.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.Streams;

/**
 * Some helper functions for the TLS API.
 */

public final class TlsUtils {

    public static final byte[] EMPTY_BYTES = new byte[0];
    public static final short[] EMPTY_SHORTS = new short[0];
    public static final int[] EMPTY_INTS = new int[0];
    public static final long[] EMPTY_LONGS = new long[0];
    public static final String[] EMPTY_STRINGS = new String[0];


    private static final BinaryReader binaryReader = new BinaryReader();
    private static final BinaryWriter binaryWriter = new BinaryWriter();
    private static final OpaqueDataUtils opaqueDataUtils = new OpaqueDataUtils();

    private TlsUtils() {

    }

    public static short readUint8(InputStream input) throws IOException {
        return binaryReader.readUint8(input);
    }

    public static short readUint8(byte[] buf, int offset) {
        return binaryReader.readUint8(buf, offset);
    }

    public static int readUint16(InputStream input) throws IOException {
        return binaryReader.readUint16(input);
    }

    public static int readUint16(byte[] buf, int offset) {
        return binaryReader.readUint16(buf, offset);
    }

    public static int readUint24(InputStream input) throws IOException {
        return binaryReader.readUint24(input);
    }

    public static int readUint24(byte[] buf, int offset) {
        return binaryReader.readUint24(buf, offset);
    }

    public static long readUint32(InputStream input) throws IOException {
        return binaryReader.readUint32(input);
    }

    public static long readUint32(byte[] buf, int offset) {
        return binaryReader.readUint32(buf, offset);
    }

    public static long readUint48(InputStream input) throws IOException {
        return binaryReader.readUint48(input);
    }


    public static byte[] readFully(int length, InputStream input) throws IOException {
        return binaryReader.readFully(input, length);
    }

    public static void writeUint8(short value, OutputStream output) throws IOException {
        binaryWriter.writeUint8(value, output);
    }

    public static void writeUint8(int value, OutputStream output) throws IOException {
        binaryWriter.writeUint8(value, output);
    }

    public static void writeUint8(short value, byte[] buf, int offset) {
        binaryWriter.writeUint8(value, buf, offset);
    }

    public static void writeUint8(int value, byte[] buf, int offset) {
        binaryWriter.writeUint8(value, buf, offset);
    }

    public static void writeUint16(int value, OutputStream output) throws IOException {
        binaryWriter.writeUint16(value, output);
    }

    public static void writeUint16(int value, byte[] buf, int offset) {
        binaryWriter.writeUint16(value, buf, offset);
    }

    public static void writeUint24(int value, OutputStream output) throws IOException {
        binaryWriter.writeUint24(value, output);
    }

    public static void writeUint24(int value, byte[] buf, int offset) {
        binaryWriter.writeUint24(value, buf, offset);
    }

    public static void writeUint32(long value, OutputStream output) throws IOException {
        binaryWriter.writeUint32(value, output);
    }

    public static void writeUint32(long value, byte[] buf, int offset) {
        binaryWriter.writeUint32(value, buf, offset);
    }

    public static void writeUint48(long value, OutputStream output) throws IOException {
        binaryWriter.writeUint48(value, output);
    }


    public static void writeOpaque8(byte[] buf, OutputStream output) throws IOException {
        opaqueDataUtils.writeOpaque8(buf, output);
    }

    public static void writeOpaque8(byte[] data, byte[] buf, int offset) throws IOException {
        opaqueDataUtils.writeOpaque8(data, buf, offset);
    }

    public static void writeOpaque16(byte[] buf, OutputStream output) throws IOException {
        opaqueDataUtils.writeOpaque16(buf, output);
    }

    public static void writeOpaque16(byte[] data, byte[] buf, int offset) throws IOException {
        opaqueDataUtils.writeOpaque16(data, buf, offset);
    }

    public static void writeOpaque24(byte[] buf, OutputStream output) throws IOException {
        opaqueDataUtils.writeOpaque24(buf, output);
    }

    public static byte[] readOpaque8(InputStream input) throws IOException {
        return opaqueDataUtils.readOpaque8(input);
    }

    public static byte[] readOpaque8(InputStream input, int minLength) throws IOException {
        return opaqueDataUtils.readOpaque8(input, minLength);
    }

    public static byte[] readOpaque16(InputStream input) throws IOException {
        return opaqueDataUtils.readOpaque16(input);
    }

    public static byte[] readOpaque16(InputStream input, int minLength) throws IOException {
        return opaqueDataUtils.readOpaque16(input, minLength);
    }

    public static boolean isValidUint8(short value) {
        return (value & 0xFF) == value;
    }

    public static boolean isValidUint8(int value) {
        return (value & 0xFF) == value;
    }

    public static boolean isValidUint8(long value) {
        return (value & 0xFFL) == value;
    }

    public static boolean isValidUint16(int value) {
        return (value & 0xFFFF) == value;
    }

    public static boolean isValidUint16(long value) {
        return (value & 0xFFFFL) == value;
    }

    public static boolean isValidUint24(int value) {
        return (value & 0xFFFFFF) == value;
    }

    public static boolean isValidUint24(long value) {
        return (value & 0xFFFFFFL) == value;
    }

    public static boolean isValidUint32(long value) {
        return (value & 0xFFFFFFFFL) == value;
    }

    public static boolean isValidUint48(long value) {
        return (value & 0xFFFFFFFFFFFFL) == value;
    }

    public static boolean isValidUint64(long value) {
        return true;
    }

    public static void writeUint8Array(short[] uints, OutputStream output) throws IOException {
        for (short uint : uints) {
            writeUint8(uint, output);
        }
    }

    public static void writeUint8Array(short[] uints, byte[] buf, int offset) throws IOException {
        for (short uint : uints) {
            writeUint8(uint, buf, offset++);
        }
    }

    public static void writeUint8ArrayWithUint8Length(short[] uints, OutputStream output) throws IOException {
        writeUint8(uints.length, output);
        writeUint8Array(uints, output);
    }

    public static void writeUint16Array(int[] uints, OutputStream output) throws IOException {
        for (int uint : uints) {
            writeUint16(uint, output);
        }
    }

    public static short[] decodeUint8ArrayWithUint8Length(byte[] buf) throws IOException {
        if (buf == null || buf.length < 1) {
            throw new IOException("AlertDescription.decode_error");
        }

        int count = readUint8(buf, 0);
        if (buf.length != (count + 1)) {
            throw new IOException("AlertDescription.decode_error");
        }

        short[] uints = new short[count];
        for (int i = 0; i < count; ++i) {
            uints[i] = readUint8(buf, i + 1);
        }
        return uints;
    }

    public static byte[] clone(byte[] data) {
        return data == null ? null : data.length == 0 ? EMPTY_BYTES : data.clone();
    }

    public static String[] clone(String[] data) {
        return data == null ? null : data.length < 1 ? EMPTY_STRINGS : data.clone();
    }

    public static boolean constantTimeAreEqual(int len, byte[] a, int aOff, byte[] b, int bOff) {
        int d = 0;
        for (int i = 0; i < len; ++i) {
            d |= (a[aOff + i] ^ b[bOff + i]);
        }
        return 0 == d;
    }

    public static byte[] copyOfRangeExact(byte[] original, int from, int to) {
        int newLength = to - from;
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0, newLength);
        return copy;
    }

    public static void writeGMTUnixTime(byte[] buf, int offset) {
        int t = (int) (System.currentTimeMillis() / 1000L);
        buf[offset] = (byte) (t >>> 24);
        buf[offset + 1] = (byte) (t >>> 16);
        buf[offset + 2] = (byte) (t >>> 8);
        buf[offset + 3] = (byte) t;
    }

    public static boolean addToSet(Vector s, int i) {
        boolean result = !s.contains(Integers.valueOf(i));
        if (result) {
            s.add(Integers.valueOf(i));
        }
        return result;
    }

    public static byte[] getExtensionData(Hashtable extensions, Integer extensionType) {
        return extensions == null ? null : (byte[]) extensions.get(extensionType);
    }

    public static ASN1Primitive readASN1Object(byte[] encoding) throws IOException {
        try (ASN1InputStream asn1 = new ASN1InputStream(encoding)) {
            ASN1Primitive result = asn1.readObject();
            if (null == result || null != asn1.readObject()) {
                throw new IOException("AlertDescription.decode_error");
            }
            return result;
        }
    }

    public static void requireDEREncoding(ASN1Object asn1, byte[] encoding) throws IOException {
        byte[] check = asn1.getEncoded(ASN1Encoding.DER);
        if (!Arrays.areEqual(check, encoding)) {
            throw new IOException("AlertDescription.decode_error");
        }
    }

    public static boolean isNullOrEmpty(byte[] array) {
        return array == null || array.length < 1;
    }

    public static boolean isNullOrEmpty(short[] array) {
        return array == null || array.length < 1;
    }

    public static boolean isNullOrEmpty(int[] array) {
        return array == null || array.length < 1;
    }

    public static boolean isNullOrEmpty(Object[] array) {
        return array == null || array.length < 1;
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.length() < 1;
    }

    public static boolean isNullOrEmpty(Vector v) {
        return v == null || v.isEmpty();
    }
}

