package org.red5.io.utils;

import org.bouncycastle.util.io.Streams;

import java.io.*;

import java.io.IOException;
import java.io.InputStream;

public final class BinaryReader {

    public short readUint8(InputStream input) throws IOException {
        int i = input.read();
        if (i < 0) throw new EOFException();
        return (short) i;
    }

    public int readUint16(InputStream input) throws IOException {
        int i1 = input.read();
        int i2 = input.read();
        if (i2 < 0) throw new EOFException();
        return (i1 << 8) | i2;
    }

    public int readUint24(InputStream input) throws IOException {
        int i1 = input.read();
        int i2 = input.read();
        int i3 = input.read();
        if (i3 < 0) throw new EOFException();
        return (i1 << 16) | (i2 << 8) | i3;
    }

    public long readUint32(InputStream input) throws IOException {
        int i1 = input.read();
        int i2 = input.read();
        int i3 = input.read();
        int i4 = input.read();
        if (i4 < 0) throw new EOFException();
        return ((long) i1 << 24) | ((long) i2 << 16) | ((long) i3 << 8) | i4;
    }

    public long readUint48(InputStream input) throws IOException {
        long hi = readUint24(input);
        long lo = readUint24(input);
        return (hi << 24) | lo;
    }

    public long readUint64(InputStream input) throws IOException {
        long hi = readUint32(input);
        long lo = readUint32(input);
        return (hi << 32) | lo;
    }

    public short readUint8(byte[] buf, int offset) {
        return (short) (buf[offset] & 0xFF);
    }

    public int readUint16(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) << 8) | (buf[offset + 1] & 0xFF);
    }

    public int readUint24(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) << 16) |
                ((buf[offset + 1] & 0xFF) << 8) |
                (buf[offset + 2] & 0xFF);
    }

    public long readUint32(byte[] buf, int offset) {
        return ((long) (buf[offset] & 0xFF) << 24) |
                ((long) (buf[offset + 1] & 0xFF) << 16) |
                ((long) (buf[offset + 2] & 0xFF) << 8) |
                (long) (buf[offset + 3] & 0xFF);
    }

    public byte[] readFully(InputStream input, int length) throws IOException {
        byte[] buf = new byte[length];
        int read = Streams.readFully(input, buf);
        if (read != length) throw new EOFException();
        return buf;
    }
}