/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.sf.ehcache.Element;
import org.apache.commons.beanutils.BeanMap;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.annotations.Anonymous;
import org.red5.compatibility.flex.messaging.io.ObjectProxy;
import org.red5.io.amf.ActionMessageFormat;
import org.red5.io.object.RecordSet;
import org.red5.io.object.Serializer;
import org.red5.io.object.UnsignedInt;
import org.red5.io.utils.HexDump;
import org.red5.io.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.red5.io.amf.Output;
import org.red5.io.amf3.AMF3Serializer;
import org.red5.io.amf3.AMF3Writer;
import java.util.*;

public class AMF3OutputWriter extends Output {
    private final AMF3Writer writer;
    private final AMF3Serializer serializer;

    public AMF3OutputWriter(IoBuffer buf) {
        super(buf);
        this.writer = new AMF3Writer(buf);
        this.serializer = new AMF3Serializer(writer);
    }

    /**
     * Enforces the use of AMF3 format in the writer.
     */
    public void enforceAMF3() {
        writer.enforceAMF3();
    }

    /**
     * Retrieves the underlying I/O buffer.
     *
     * @return IoBuffer being used by the writer.
     */
    public IoBuffer getBuffer() {
        return writer.getBuffer();
    }

    /**
     * Writes a null value to the output.
     */
    @Override
    public void writeNull() {
        serializer.serializeNull();
    }

    /**
     * Writes a boolean value to the output.
     *
     * @param bol The Boolean value to write.
     */
    @Override
    public void writeBoolean(Boolean bol) {
        serializer.serializeBoolean(bol);
    }

    /**
     * Writes a numeric value to the output.
     *
     * @param num The Number value to write.
     */
    @Override
    public void writeNumber(Number num) {
        serializer.serializeNumber(num);
    }

    /**
     * Writes a string value to the output.
     *
     * @param string The String value to write.
     */
    @Override
    public void writeString(String string) {
        serializer.serializeString(string);
    }

    /**
     * Writes a Vector of Integers to the output.
     *
     * @param vector The Vector of Integers to write.
     */
    @Override
    public void writeVectorInt(Vector<Integer> vector) {
        serializer.serializeVectorInt(vector);
    }

    /**
     * Writes a Vector of unsigned integers to the output.
     *
     * @param vector The Vector of Long values to write as unsigned integers.
     */
    @Override
    public void writeVectorUInt(Vector<Long> vector) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_VECTOR_UINT);

        if (writer.hasReference(vector)) {
            writer.writeInteger(writer.getReferenceId(vector) << 1);
            return;
        }

        writer.storeReference(vector);
        writer.writeInteger(vector.size() << 1 | 1);
        writer.getBuffer().put((byte) 0x00);
        for (Long value : vector) {
            writer.getBuffer().put(new UnsignedInt(value).getBytes());
        }
    }

    /**
     * Writes an XML document to the output.
     *
     * @param xml The XML Document to write.
     */
    @Override
    public void writeXML(Document xml) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_XML);

        if (writer.hasReference(xml)) {
            writer.writeInteger(writer.getReferenceId(xml) << 1);
            return;
        }

        String xmlString = XMLUtils.docToString(xml);
        byte[] encoded = xmlString.getBytes(StandardCharsets.UTF_8);
        writer.writeInteger(encoded.length << 1 | 1);
        writer.getBuffer().put(encoded);
        writer.storeReference(xml);
    }

    /**
     * Writes a ByteArray to the output.
     *
     * @param array The ByteArray to write.
     */
    @Override
    public void writeByteArray(ByteArray array) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_BYTEARRAY);

        if (writer.hasReference(array)) {
            writer.writeInteger(writer.getReferenceId(array) << 1);
            return;
        }

        writer.storeReference(array);
        IoBuffer data = array.getData();
        writer.writeInteger(data.limit() << 1 | 1);
        byte[] tmp = new byte[data.limit()];
        int oldPos = data.position();
        try {
            data.position(0);
            data.get(tmp);
            writer.getBuffer().put(tmp);
        } finally {
            data.position(oldPos);
        }
    }

    /**
     * Writes an array object to the output.
     *
     * @param array The array object to write.
     */
    @Override
    public void writeArray(Object array) {
        Class<?> componentType = array.getClass().getComponentType();
        if (componentType.equals(byte.class)) {
            writePrimitiveByteArray((byte[]) array);
        } else {
            serializer.serializeArray((Object[]) array);
        }
    }

    /**
     * Writes a primitive byte array to the output.
     *
     * @param bytes The byte array to write.
     */
    private void writePrimitiveByteArray(byte[] bytes) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_BYTEARRAY);

        if (writer.hasReference(bytes)) {
            writer.writeInteger(writer.getReferenceId(bytes) << 1);
            return;
        }

        writer.storeReference(bytes);
        writer.writeInteger(bytes.length << 1 | 1);
        writer.getBuffer().put(bytes);
    }
}