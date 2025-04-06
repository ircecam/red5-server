package org.red5.io.utils;

import java.io.IOException;
import java.io.OutputStream;

import java.io.IOException;
import java.io.OutputStream;

public final class BinaryWriter {

    public void writeUint8(int value, OutputStream output) throws IOException {
        output.write(value & 0xFF);
    }

    public void writeUint16(int value, OutputStream output) throws IOException {
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    public void writeUint24(int value, OutputStream output) throws IOException {
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    public void writeUint32(long value, OutputStream output) throws IOException {
        output.write((int) ((value >>> 24) & 0xFF));
        output.write((int) ((value >>> 16) & 0xFF));
        output.write((int) ((value >>> 8) & 0xFF));
        output.write((int) (value & 0xFF));
    }

    public void writeUint48(long value, OutputStream output) throws IOException {
        output.write((int) ((value >>> 40) & 0xFF));
        output.write((int) ((value >>> 32) & 0xFF));
        writeUint32(value, output);
    }

    public void writeUint64(long value, OutputStream output) throws IOException {
        writeUint32(value >>> 32, output);
        writeUint32(value, output);
    }

    public void writeUint8(int value, byte[] buf, int offset) {
        buf[offset] = (byte) value;
    }

    public void writeUint16(int value, byte[] buf, int offset) {
        buf[offset] = (byte) (value >>> 8);
        buf[offset + 1] = (byte) value;
    }

    public void writeUint24(int value, byte[] buf, int offset) {
        buf[offset] = (byte) (value >>> 16);
        buf[offset + 1] = (byte) (value >>> 8);
        buf[offset + 2] = (byte) value;
    }

    public void writeUint32(long value, byte[] buf, int offset) {
        buf[offset] = (byte) (value >>> 24);
        buf[offset + 1] = (byte) (value >>> 16);
        buf[offset + 2] = (byte) (value >>> 8);
        buf[offset + 3] = (byte) value;
    }

    public void writeBytes(byte[] data, OutputStream output) throws IOException {
        output.write(data);
    }
}