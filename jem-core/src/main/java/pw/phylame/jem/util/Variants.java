/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pw.phylame.jem.util;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final Collection<String> typeNames = new LinkedHashSet<>();

    private static final Map<Class<?>, String> typeMapping = new ConcurrentHashMap<>();

    private static void initVariants() {
        try {
            val prop = CollectUtils.propertiesFor("!pw/phylame/jem/util/variants.properties");
            if (prop != null) {
                String type;
                for (val e : prop.entrySet()) {
                    type = e.getValue().toString();
                    typeMapping.put(Class.forName(e.getKey().toString()), type);
                    typeNames.add(type);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e("Variants", e);
        }
    }

    static {
        initVariants();
    }

    /**
     * Returns supported type by Jem.
     *
     * @return array of type names
     */
    public static String[] supportedTypes() {
        return typeNames.toArray(new String[typeNames.size()]);
    }

    /**
     * Gets readable text for variant type.
     *
     * @param type name of variant
     * @return the text, or {@literal null} if the type is unknown
     */
    public static String titleOf(@NonNull String type) {
        try {
            return M.tr("variant." + type);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    /**
     * Registers user specified type.
     *
     * @param type name of type
     */
    public static void registerType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("type cannot be null or empty");
        }
        if (typeNames.contains(type)) {
            throw Exceptions.forIllegalArgument("type %s already registered", type);
        }
        typeNames.add(type);
    }

    public static String checkType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("type cannot be null or empty");
        }
        if (!typeNames.contains(type)) {
            throw Exceptions.forIllegalArgument("type %s in unsupported", type);
        }
        return type;
    }

    /**
     * Maps specified class for variant type.
     *
     * @param clazz the class
     * @param type  name of type
     * @throws NullPointerException     if the class is {@literal null}
     * @throws IllegalArgumentException if the name of type is invalid
     */
    public static void mapType(@NonNull Class<?> clazz, String type) {
        typeMapping.put(clazz, checkType(type));
    }

    /**
     * Detects the type of specified object.
     *
     * @param obj the object
     * @return the type name or {@literal null} if unknown
     * @throws NullPointerException if the object is {@literal null}
     */
    public static String typeOf(@NonNull final Object obj) {
        return CollectUtils.getOrPut(typeMapping, obj.getClass(), false, new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> clazz) {
                for (val e : typeMapping.entrySet()) {
                    if (e.getKey().isAssignableFrom(clazz)) {
                        return e.getValue();
                    }
                }
                return null;
            }
        });
    }

    /**
     * Returns default value for specified type.
     *
     * @param type name of type
     * @return the value, or {@literal null} if the type not in jem built-in types
     * @throws IllegalArgumentException if the name of type is empty
     */
    public static Object defaultFor(String type) {
        switch (checkType(type)) {
            case STRING:
                return StringUtils.EMPTY_TEXT;
            case TEXT:
                return Texts.forEmpty(Texts.PLAIN);
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
                return null;
        }
    }

    /**
     * Converts specified object to string for printing.
     *
     * @param obj the object or {@literal null} if the type of object is unknown
     * @return a string represent the object
     */
    public static String printable(@NonNull Object obj) {
        if (obj instanceof CharSequence) {
            return obj.toString();
        } else if (obj instanceof Text) {
            return ((Text) obj).getText();
        } else if (obj instanceof Date) {
            return DateUtils.format((Date) obj, "yy-M-d");
        } else if (obj instanceof Locale) {
            return ((Locale) obj).getDisplayName();
        } else if (obj instanceof Flob) {
            return obj.toString();
        } else {
            return null;
        }
    }
}
