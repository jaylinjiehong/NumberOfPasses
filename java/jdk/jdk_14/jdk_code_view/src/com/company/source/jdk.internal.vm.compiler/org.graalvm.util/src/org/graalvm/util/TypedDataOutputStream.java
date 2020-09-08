/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A stream that can write (trivial) values together with their data type, for use with
 * {@link TypedDataInputStream}.
 */
public class TypedDataOutputStream extends DataOutputStream {
    /** Determines if {@code value} is supported by {@link #writeTypedValue(Object)}. */
    public static boolean isValueSupported(Object value) {
        if (value == null) {
            return false;
        }
        Class<?> valueClass = value.getClass();
        return valueClass == Boolean.class ||
                        valueClass == Byte.class ||
                        valueClass == Short.class ||
                        valueClass == Character.class ||
                        valueClass == Integer.class ||
                        valueClass == Long.class ||
                        valueClass == Float.class ||
                        valueClass == Double.class ||
                        valueClass == String.class ||
                        value.getClass().isEnum();
    }

    public TypedDataOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Writes the value that is represented by the given non-null object, together with information
     * on the value's data type.
     *
     * @param value A value of a {@linkplain #isValueSupported supported type}.
     * @exception IllegalArgumentException when the provided type is not supported.
     * @exception IOException in case of an I/O error.
     */
    public void writeTypedValue(Object value) throws IOException {
        Class<?> valueClz = value.getClass();
        if (valueClz == Boolean.class) {
            this.writeByte('Z');
            this.writeBoolean((Boolean) value);
        } else if (valueClz == Byte.class) {
            this.writeByte('B');
            this.writeByte((Byte) value);
        } else if (valueClz == Short.class) {
            this.writeByte('S');
            this.writeShort((Short) value);
        } else if (valueClz == Character.class) {
            this.writeByte('C');
            this.writeChar((Character) value);
        } else if (valueClz == Integer.class) {
            this.writeByte('I');
            this.writeInt((Integer) value);
        } else if (valueClz == Long.class) {
            this.writeByte('J');
            this.writeLong((Long) value);
        } else if (valueClz == Float.class) {
            this.writeByte('F');
            this.writeFloat((Float) value);
        } else if (valueClz == Double.class) {
            this.writeByte('D');
            this.writeDouble((Double) value);
        } else if (valueClz == String.class) {
            this.writeByte('U');
            this.writeUTF((String) value);
        } else if (valueClz.isEnum()) {
            this.writeByte('U');
            this.writeUTF(((Enum<?>) value).name());
        } else {
            throw new IllegalArgumentException(String.format("Unsupported type: Value: %s, Value type: %s", value, valueClz));
        }
    }
}
