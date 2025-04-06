package org.red5.io.utils;

import java.io.IOException;
import java.io.OutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class OpaqueDataUtils {
    private final BinaryReader reader = new BinaryReader();
    private final BinaryWriter writer = new BinaryWriter();

    public void writeOpaque8(byte[] data, OutputStream output) throws IOException {
        writer.writeUint8(data.length, output);
        writer.writeBytes(data, output);
    }

    public void writeOpaque16(byte[] data, OutputStream output) throws IOException {
        writer.writeUint16(data.length, output);
        writer.writeBytes(data, output);
    }

    public void writeOpaque24(byte[] data, OutputStream output) throws IOException {
        writer.writeUint24(data.length, output);
        writer.writeBytes(data, output);
    }

    public void writeOpaque8(byte[] data, byte[] buf, int offset) throws IOException {
        writer.writeUint8(data.length, buf, offset);
        System.arraycopy(data, 0, buf, offset + 1, data.length);
    }

    public void writeOpaque16(byte[] data, byte[] buf, int offset) throws IOException {
        writer.writeUint16(data.length, buf, offset);
        System.arraycopy(data, 0, buf, offset + 2, data.length);
    }

    public byte[] readOpaque8(InputStream input) throws IOException {
        return readOpaque8(input, 0);
    }

    public byte[] readOpaque8(InputStream input, int minLength) throws IOException {
        int length = reader.readUint8(input);
        if (length < minLength) throw new IOException("Invalid length");
        return reader.readFully(input, length);
    }

    public byte[] readOpaque16(InputStream input) throws IOException {
        return readOpaque16(input, 0);
    }

    public byte[] readOpaque16(InputStream input, int minLength) throws IOException {
        int length = reader.readUint16(input);
        if (length < minLength) throw new IOException("Invalid length");
        return reader.readFully(input, length);
    }
}