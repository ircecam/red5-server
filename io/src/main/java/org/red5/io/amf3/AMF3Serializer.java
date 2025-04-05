package org.red5.io.amf3;

import org.red5.annotations.Anonymous;
import org.red5.compatibility.flex.messaging.io.ObjectProxy;
import org.red5.io.object.Serializer;
import org.w3c.dom.Document;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * The AMF3Serializer class provides functionality for serializing Java objects
 * into Action Message Format 3 (AMF3). This format is commonly used for data exchange
 * in Adobe Flash environments or systems that require efficient binary serialization.
 * The class supports serialization of primitive types, complex data structures, 
 * and custom objects as specified by the AMF3 standard.
 *
 * The serialization process directly writes the serialized output to an underlying
 * buffer through an instance of the AMF3Writer class.
 */

public class AMF3Serializer {
    private final AMF3Writer writer;
    private final ConcurrentMap<String, Integer> stringReferences;

    public AMF3Serializer(AMF3Writer writer) {
        this.writer = writer;
        this.stringReferences = writer.stringReferences;
    }

    /**
     * Serializes a boolean value in AMF3 format.
     */
    public void serializeBoolean(Boolean bol) {
        writer.writeAMF3Header();
        writer.getBuffer().put(bol ? AMF3.TYPE_BOOLEAN_TRUE : AMF3.TYPE_BOOLEAN_FALSE);
    }

    /**
     * Serializes a null value in AMF3 format.
     */
    public void serializeNull() {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_NULL);
    }

    /**
     * Serializes a numeric value in AMF3 format, handling both integers and doubles.
     */
    public void serializeNumber(Number num) {
        writer.writeAMF3Header();
        long longValue = num.longValue();

        if (longValue >= AMF3.MIN_INTEGER_VALUE && longValue <= AMF3.MAX_INTEGER_VALUE) {
            writer.getBuffer().put(AMF3.TYPE_INTEGER);
            writer.writeInteger(longValue);
        } else {
            writer.getBuffer().put(AMF3.TYPE_NUMBER);
            writer.getBuffer().putDouble(num.doubleValue());
        }
    }

    /**
     * Serializes a string in AMF3 format, with reference tracking.
     */
    public void serializeString(String string) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_STRING);

        if (string.isEmpty()) {
            writer.writeInteger(1);
            return;
        }

        if (stringReferences.containsKey(string)) {
            writer.writeInteger(stringReferences.get(string) << 1);
        } else {
            byte[] encoded = string.getBytes(StandardCharsets.UTF_8);
            writer.writeInteger(encoded.length << 1 | 1);
            writer.getBuffer().put(encoded);
            stringReferences.put(string, stringReferences.size());
        }
    }

    /**
     * Serializes a date object in AMF3 format.
     */
    public void serializeDate(Date date) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_DATE);

        if (writer.hasReference(date)) {
            writer.writeInteger(writer.getReferenceId(date) << 1);
            return;
        }

        writer.storeReference(date);
        writer.writeInteger(1);
        writer.getBuffer().putDouble(date.getTime());
    }

    /**
     * Serializes an array in AMF3 format.
     */
    public void serializeArray(Object[] array) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_ARRAY);

        if (writer.hasReference(array)) {
            writer.writeInteger(writer.getReferenceId(array) << 1);
            return;
        }

        writer.storeReference(array);
        writer.enforceAMF3();
        try {
            writer.writeInteger(array.length << 1 | 1);
            serializeString("");
            for (Object item : array) {
                serializeObject(item);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writer.enforceAMF3();
        }
    }

    /**
     * Serializes a generic object in AMF3 format.
     */
    public void serializeObject(Object object) throws IOException {
        if (object == null) {
            serializeNull();
            return;
        }

        Class<?> objectClass = object.getClass();

        if (object instanceof String) {
            serializeString((String) object);
        } else if (object instanceof Number) {
            serializeNumber((Number) object);
        } else if (object instanceof Boolean) {
            serializeBoolean((Boolean) object);
        } else if (object instanceof Date) {
            serializeDate((Date) object);
        } else if (object instanceof Map) {
            serializeMap((Map<Object, Object>) object);
        } else if (object instanceof Collection) {
            serializeCollection((Collection<?>) object);
        } else if (objectClass.isArray()) {
            serializeArray((Object[]) object);
        } else {
            serializeCustomObject(object);
        }
    }

    /**
     * Serializes a custom object in AMF3 format using reflection.
     */
    private void serializeCustomObject(Object object) throws IOException {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_OBJECT);

        if (writer.hasReference(object)) {
            writer.writeInteger(writer.getReferenceId(object) << 1);
            return;
        }

        writer.storeReference(object);

        if (object instanceof IExternalizable) {
            serializeExternalizable((IExternalizable) object);
            return;
        }

        int type = AMF3.TYPE_OBJECT_VALUE << 2 | 1 << 1 | 1;
        writer.writeInteger(type);

        if (!object.getClass().isAnnotationPresent(Anonymous.class)) {
            serializeString(Serializer.getClassName(object.getClass()));
        } else {
            serializeString("");
        }

        writer.enforceAMF3();
        try {
            for (Field field : object.getClass().getFields()) {
                if (!Modifier.isTransient(field.getModifiers())) {
                    serializeString(field.getName());
                    serializeObject(field.get(object));
                }
            }
            serializeString("");
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to serialize object", e);
        } finally {
            writer.enforceAMF3();
        }
    }

    /**
     * Serializes a vector of integers in AMF3 format.
     */
    public void serializeVectorInt(Vector<Integer> vector) {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_VECTOR_INT);

        if (writer.hasReference(vector)) {
            writer.writeInteger(writer.getReferenceId(vector) << 1);
            return;
        }

        writer.storeReference(vector);
        writer.writeInteger(vector.size() << 1 | 1);
        writer.getBuffer().put((byte) 0x00);
        for (Integer value : vector) {
            writer.getBuffer().putInt(value);
        }
    }

    /**
     * Serializes a collection in AMF3 format.
     */
    public void serializeCollection(Collection<?> collection) throws IOException {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_ARRAY);

        if (writer.hasReference(collection)) {
            writer.writeInteger(writer.getReferenceId(collection) << 1);
            return;
        }

        writer.storeReference(collection);
        writer.enforceAMF3();
        try {
            writer.writeInteger(collection.size() << 1 | 1);
            serializeString("");
            for (Object item : collection) {
                serializeObject(item);
            }
        } finally {
            writer.enforceAMF3();
        }
    }

    /**
     * Serializes a map in AMF3 format.
     */
    public void serializeMap(Map<Object, Object> map) throws IOException {
        writer.writeAMF3Header();
        writer.getBuffer().put(AMF3.TYPE_OBJECT);

        if (writer.hasReference(map)) {
            writer.writeInteger(writer.getReferenceId(map) << 1);
            return;
        }

        writer.storeReference(map);

        int count = 0;
        for (int i = 0; i < map.size(); i++) {
            try {
                if (!map.containsKey(i)) break;
            } catch (ClassCastException e) {
                break;
            }
            count++;
        }

        writer.enforceAMF3();
        try {
            if (count == map.size()) {
                writer.writeInteger(count << 1 | 1);
                serializeString("");
                for (int i = 0; i < count; i++) {
                    serializeObject(map.get(i));
                }
                return;
            }

            writer.writeInteger(count << 1 | 1);

            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object key = entry.getKey();
                if ((key instanceof Number) && !(key instanceof Float) && !(key instanceof Double) &&
                        ((Number) key).longValue() >= 0 && ((Number) key).longValue() < count) {
                    continue;
                }
                serializeString(key.toString());
                serializeObject(entry.getValue());
            }

            serializeString("");
            for (int i = 0; i < count; i++) {
                serializeObject(map.get(i));
            }
        } finally {
            writer.enforceAMF3();
        }
    }

    /**
     * Serializes an externalizable object in AMF3 format.
     */
    public void serializeExternalizable(IExternalizable object) throws IOException {
        int type = 1 << 1 | 1;
        if (object instanceof ObjectProxy) {
            type |= AMF3.TYPE_OBJECT_PROXY << 2;
        } else {
            type |= AMF3.TYPE_OBJECT_EXTERNALIZABLE << 2;
        }

        writer.writeInteger(type);
        serializeString(Serializer.getClassName(object.getClass()));

        writer.enforceAMF3();
    }

    /**
     * Serializes an arbitrary object in AMF3 format using reflection.
     */
    protected void serializeArbitraryObject(Object object) throws IOException {
        Class<?> objectClass = object.getClass();

        if (!objectClass.isAnnotationPresent(Anonymous.class)) {
            serializeString(Serializer.getClassName(objectClass));
        } else {
            serializeString("");
        }

        writer.enforceAMF3();
        try {
            for (Field field : objectClass.getFields()) {
                if (!Modifier.isTransient(field.getModifiers())) {
                    String fieldName = field.getName();
                    serializeString(fieldName);
                    Object value = field.get(object);
                    serializeObject(value);
                }
            }
            serializeString("");
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to serialize object field", e);
        } finally {
            writer.enforceAMF3();
        }
    }
}