/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pw.phylame.jem.util;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.util.Provider;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.value.Lazy;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Variants {
    private Variants() {
    }

    // declare variant type aliases
    public static final String FLOB = "file";
    public static final String TEXT = "text";
    public static final String STRING = "str";
    public static final String INTEGER = "int";
    public static final String REAL = "real";
    public static final String LOCALE = "locale";
    public static final String DATETIME = "datetime";
    public static final String BOOLEAN = "bool";

    /**
     * Returns supported type by Jem.
     *
     * @return array of type names
     */
    public static String[] supportedTypes() {
        return new String[]{FLOB, TEXT, STRING, INTEGER, REAL, LOCALE, DATETIME, BOOLEAN};
    }

    private static final Map<Class<?>, String> typeNames = new HashMap<>();

    static {
        typeNames.put(Character.class, STRING);
        typeNames.put(String.class, STRING);
        typeNames.put(Date.class, DATETIME);
        typeNames.put(Locale.class, LOCALE);
        typeNames.put(Byte.class, INTEGER);
        typeNames.put(Short.class, INTEGER);
        typeNames.put(Integer.class, INTEGER);
        typeNames.put(Long.class, INTEGER);
        typeNames.put(Boolean.class, BOOLEAN);
        typeNames.put(Float.class, REAL);
        typeNames.put(Double.class, REAL);
    }

    public static void mapType(@NonNull Class<?> clazz, @NonNull String type) {
        typeNames.put(clazz, type);
    }

    /**
     * Returns type name of specified object.
     *
     * @param o attribute value
     * @return the type name
     * @throws NullPointerException if the obj is <code>null</code>
     */
    public static String typeOf(@NonNull Object o) {
        val type = typeNames.get(o.getClass());
        if (type != null) {
            return type;
        }
        if (o instanceof CharSequence) {
            return STRING;
        } else if (o instanceof Text) {
            return TEXT;
        } else if (o instanceof Flob) {
            return FLOB;
        } else if (o instanceof Date) {
            return DATETIME;
        } else if (o instanceof Locale) {
            return LOCALE;
        } else {
            return STRING;
        }
    }

    /**
     * Returns default value of specified type.
     *
     * @param type type string
     * @return the value
     */
    public static Object defaultFor(String type) {
        switch (type) {
            case STRING:
                return StringUtils.EMPTY_TEXT;
            case TEXT:
                return Texts.forEmpty(Text.PLAIN);
            case FLOB:
                return Flobs.forEmpty("_empty_", PathUtils.UNKNOWN_MIME);
            case DATETIME:
                return new Date();
            case LOCALE:
                return Locale.getDefault();
            case INTEGER:
                return 0;
            case REAL:
                return 0.0D;
            case BOOLEAN:
                return Boolean.FALSE;
            default:
                return StringUtils.EMPTY_TEXT;
        }
    }

    private static final Lazy<DateFormat> shortDateFormatter = new Lazy<>(new Provider<DateFormat>() {
        @Override
        public DateFormat provide() throws Exception {
            return new SimpleDateFormat("yy-M-d");
        }
    });

    /**
     * Converts specified object to string.
     *
     * @param o the object
     * @return a string represent the object
     */
    public static String format(@NonNull Object o) {
        if (o instanceof CharSequence) {
            return o.toString();
        } else if (o instanceof Text) {
            return ((Text) o).getText();
        } else if (o instanceof Date) {
            return shortDateFormatter.get().format((Date) o);
        } else if (o instanceof Locale) {
            return ((Locale) o).getDisplayName();
        } else {
            return o.toString();
        }
    }

    private static final Map<String, String> attributeTypes = new HashMap<>();

    static {
        attributeTypes.put(Attributes.COVER, FLOB);
        attributeTypes.put(Attributes.INTRO, TEXT);
        attributeTypes.put(Attributes.WORDS, INTEGER);
        attributeTypes.put(Attributes.DATE, DATETIME);
        attributeTypes.put(Attributes.LANGUAGE, LOCALE);
    }

    public static void mapAttributeType(String name, String type) {
        attributeTypes.put(name, type);
    }

    /**
     * Returns type of specified attribute name.
     *
     * @param name name of attribute
     * @return the type string
     */
    public static String typeOfAttribute(String name) {
        String type = attributeTypes.get(name);
        return type != null ? type : STRING;
    }
}
