package org.red5.io.utils;

import org.bouncycastle.asn1.ASN1Primitive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

public final class TlsUtilsFacade {
    private final BinaryReader binaryReader = new BinaryReader();
    private final BinaryWriter binaryWriter = new BinaryWriter();
    private final ValidationUtils validationUtils = new ValidationUtils();
    private final OpaqueDataUtils opaqueDataUtils = new OpaqueDataUtils();
    private final ArrayTtlUtils arrayUtils = new ArrayTtlUtils();

    public static final byte[] EMPTY_BYTES = ArrayTtlUtils.EMPTY_BYTES;
    public static final short[] EMPTY_SHORTS = ArrayTtlUtils.EMPTY_SHORTS;
    public static final int[] EMPTY_INTS = ArrayTtlUtils.EMPTY_INTS;
    public static final long[] EMPTY_LONGS = new long[0];
    public static final String[] EMPTY_STRINGS = new String[0];

    private TlsUtilsFacade() {
    }

    private static class Holder {
        private static final TlsUtilsFacade INSTANCE = new TlsUtilsFacade();
    }

    public static TlsUtilsFacade getInstance() {
        return Holder.INSTANCE;
    }

    public short readUint8(InputStream input) throws IOException {
        return binaryReader.readUint8(input);
    }

    public int readUint16(InputStream input) throws IOException {
        return binaryReader.readUint16(input);
    }

    public int readUint24(InputStream input) throws IOException {
        return binaryReader.readUint24(input);
    }

    public long readUint32(InputStream input) throws IOException {
        return binaryReader.readUint32(input);
    }

    public long readUint48(InputStream input) throws IOException {
        return binaryReader.readUint48(input);
    }

    public byte[] readFully(InputStream input, int length) throws IOException {
        return binaryReader.readFully(input, length);
    }

    public short readUint8(byte[] buf, int offset) {
        return binaryReader.readUint8(buf, offset);
    }

    public int readUint16(byte[] buf, int offset) {
        return binaryReader.readUint16(buf, offset);
    }

    public void writeUint8(int value, OutputStream output) throws IOException {
        binaryWriter.writeUint8(value, output);
    }

    public void writeUint16(int value, OutputStream output) throws IOException {
        binaryWriter.writeUint16(value, output);
    }

    public void writeUint24(int value, OutputStream output) throws IOException {
        binaryWriter.writeUint24(value, output);
    }

    public void writeUint32(long value, OutputStream output) throws IOException {
        binaryWriter.writeUint32(value, output);
    }

    public void writeUint48(long value, OutputStream output) throws IOException {
        binaryWriter.writeUint48(value, output);
    }

    public void writeUint8(int value, byte[] buf, int offset) {
        binaryWriter.writeUint8(value, buf, offset);
    }

    public void writeUint16(int value, byte[] buf, int offset) {
        binaryWriter.writeUint16(value, buf, offset);
    }

    public void writeOpaque8(byte[] data, OutputStream output) throws IOException {
        opaqueDataUtils.writeOpaque8(data, output);
    }

    public void writeOpaque16(byte[] data, OutputStream output) throws IOException {
        opaqueDataUtils.writeOpaque16(data, output);
    }

    public void writeOpaque24(byte[] data, OutputStream output) throws IOException {
        opaqueDataUtils.writeOpaque24(data, output);
    }

    public byte[] readOpaque8(InputStream input) throws IOException {
        return opaqueDataUtils.readOpaque8(input);
    }

    public byte[] readOpaque16(InputStream input) throws IOException {
        return opaqueDataUtils.readOpaque16(input);
    }

    public boolean isValidUint8(long value) {
        return validationUtils.isValidUint8(value);
    }

    public boolean isValidUint16(long value) {
        return validationUtils.isValidUint16(value);
    }

    public boolean isValidUint24(long value) {
        return validationUtils.isValidUint24(value);
    }

    public boolean isValidUint32(long value) {
        return validationUtils.isValidUint32(value);
    }

    public byte[] clone(byte[] data) {
        return arrayUtils.clone(data);
    }

    public boolean contains(int[] array, int value) {
        return arrayUtils.contains(array, value);
    }

    public short[] truncate(short[] array, int newLength) {
        return arrayUtils.truncate(array, newLength);
    }

    public void writeUint8ArrayWithUint8Length(short[] uints, OutputStream output) throws IOException {
        writeUint8(uints.length, output);
        for (short uint : uints) {
            writeUint8(uint, output);
        }
    }

    public short[] readUint8ArrayWithUint8Length(InputStream input) throws IOException {
        int count = readUint8(input);
        short[] uints = new short[count];
        for (int i = 0; i < count; i++) {
            uints[i] = readUint8(input);
        }
        return uints;
    }

    public boolean isNullOrEmpty(byte[] array) {
        return array == null || array.length == 0;
    }

    public boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public boolean addToSet(Vector<Integer> s, int i) {
        boolean result = !s.contains(i);
        if (result) {
            s.add(i);
        }
        return result;
    }
}
