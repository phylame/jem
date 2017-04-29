/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

package jem.epm;

import jem.Book;
import jem.util.JemException;
import jem.util.UnsupportedFormatException;
import lombok.NonNull;
import lombok.val;
import pw.phylame.commons.function.BiFunction;
import pw.phylame.commons.io.PathUtils;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.Implementor;
import pw.phylame.commons.util.MiscUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static pw.phylame.commons.util.CollectionUtils.isEmpty;
import static pw.phylame.commons.util.CollectionUtils.setOf;

public final class EpmManager {
    private EpmManager() {
    }

    private static final String TAG = EpmManager.class.getSimpleName();

    /**
     * The default format of Jem.
     */
    public static final String PMAB = "pmab";

    /**
     * File path of parser registration
     */
    public static final String PARSER_DEFINE_FILE = "META-INF/jem/parsers.prop";

    /**
     * File path of maker registration
     */
    public static final String MAKER_DEFINE_FILE = "META-INF/jem/makers.prop";

    /**
     * Name of system property for auto-loading customized parsers and parsers.
     */
    public static final String AUTO_LOAD_KEY = "jem.epm.autoLoad";

    /**
     * Holds registered {@code Parser} class information.
     */
    private static final Implementor<Parser> parsers = new Implementor<>(Parser.class, true);

    /**
     * Holds registered {@code Maker} class information.
     */
    private static final Implementor<Maker> makers = new Implementor<>(Maker.class, true);

    /**
     * Mapping parser and maker name to file extension nameMap.
     */
    private static final Map<String, Set<String>> extensionMap = new ConcurrentHashMap<>();

    /**
     * Mapping file extension name to parser and maker name.
     */
    private static final Map<String, String> nameMap = new ConcurrentHashMap<>();

    /**
     * Registers parser class with specified name.
     * <p>
     * If parser class with same name exists, replaces the old with the new parser class.
     * </p>
     * <p>
     * NOTE: old parser and cached parser with the name will be removed.
     * </p>
     *
     * @param name name of the parser (normally the extension name of book file)
     * @param path path of the parser class
     * @throws IllegalArgumentException if the {@code name} or {@code path} is {@literal null} or empty string
     */
    public static void registerParser(String name, String path) {
        parsers.register(name, path);
    }

    /**
     * Registers parser class with specified name.
     * <p>
     * If parser class with same name exists, replaces the old with the new parser class.
     * </p>
     *
     * @param name  name of the parser (normally the extension name of book file)
     * @param clazz the {@code Parser} class
     * @throws IllegalArgumentException if the {@code name} is {@literal null} or empty string
     * @throws NullPointerException     if the {@code clazz} is {@literal null}
     */
    public static void registerParser(String name, Class<? extends Parser> clazz) {
        parsers.register(name, clazz);
    }

    /**
     * Removes registered parser with specified name.
     *
     * @param name name of the parser
     */
    public static void removeParser(String name) {
        parsers.remove(name);
    }

    /**
     * Tests parser with specified name is registered or not.
     *
     * @param name the name of format
     * @return {@literal true} if the parser is registered otherwise {@literal false}
     */
    public static boolean hasParser(String name) {
        return parsers.contains(name);
    }

    /**
     * Returns nameMap of registered parser class.
     *
     * @return sequence of format nameMap
     */
    public static Set<String> supportedParsers() {
        return parsers.names();
    }

    /**
     * Returns parser instance with specified name.
     *
     * @param name name of the parser
     * @return {@code Parser} instance or {@literal null} if parser not registered
     * @throws NullPointerException         if the {@code name} is {@literal null}
     * @throws ReflectiveOperationException if cannot create the parser
     */
    public static Parser parserFor(String name) throws ReflectiveOperationException {
        return parsers.getInstance(name);
    }

    /**
     * Registers maker class with specified name.
     * <p>
     * If maker class with same name exists, replaces the old with the new maker class.
     * </p>
     * <p>
     * NOTE: old maker and cached maker with the name will be removed.
     * </p>
     *
     * @param name name of the maker (normally the extension name of book file)
     * @param path class path of the maker class
     * @throws IllegalArgumentException if the {@code name} or {@code path} is {@literal null} or empty string
     */
    public static void registerMaker(String name, String path) {
        makers.register(name, path);
    }

    /**
     * Registers maker class with specified name.
     * <p>
     * If maker class with same name exists, replaces the old with the new maker class.
     * </p>
     *
     * @param name  name of the maker (normally the extension name of book file)
     * @param clazz the {@code Maker} class
     * @throws IllegalArgumentException if the {@code name} is {@literal null} or empty string
     * @throws NullPointerException     if the {@code clazz} is {@literal null}
     */
    public static void registerMaker(String name, Class<? extends Maker> clazz) {
        makers.register(name, clazz);
    }

    /**
     * Removes registered maker with specified name.
     *
     * @param name name of the maker
     */
    public static void removeMaker(String name) {
        makers.remove(name);
    }

    /**
     * Tests maker with specified name is registered or not.
     *
     * @param name the name of format
     * @return {@literal true} if the maker is registered otherwise {@literal false}
     */
    public static boolean hasMaker(String name) {
        return makers.contains(name);
    }

    /**
     * Returns nameMap of registered maker class.
     *
     * @return sequence of format nameMap
     */
    public static Set<String> supportedMakers() {
        return makers.names();
    }

    /**
     * Returns maker instance with specified name.
     *
     * @param name name of the maker
     * @return {@code Maker} instance or {@literal null} if maker not registered
     * @throws NullPointerException         if the {@code name} is {@literal null}
     * @throws ReflectiveOperationException if cannot create the maker
     */
    public static Maker makerFor(String name) throws ReflectiveOperationException {
        return makers.getInstance(name);
    }

    /**
     * Maps specified file extension nameMap to parser (or maker) name.
     *
     * @param name       the name of parser or maker
     * @param extensions file extension nameMap supported by the parser (or maker), do nothing if {@literal null}
     * @throws NullPointerException if the {@code name} is {@literal null}
     */
    public static void mapExtensions(@NonNull String name, Collection<String> extensions) {
        if (isEmpty(extensions)) {
            return;
        }
        Set<String> current = extensionMap.get(name);
        if (current == null) {
            extensionMap.put(name, current = new LinkedHashSet<>());
        }
        current.addAll(extensions);
        for (val ext : current) {
            nameMap.put(ext, name);
        }
    }

    /**
     * Gets supported file extension nameMap of specified parser or maker name.
     *
     * @param name the name of parser or maker
     * @return the string set of extension name
     */
    public static String[] extensionsOfName(String name) {
        val extensions = extensionMap.get(name);
        return extensions.toArray(new String[extensions.size()]);
    }

    /**
     * Gets parser or maker name by file extension name.
     *
     * @param extension the extension name
     * @return the name or {@literal null} if the extension name is unknown.
     */
    public static String nameOfExtension(String extension) {
        return nameMap.get(extension);
    }

    /**
     * Gets the format of specified file path.
     *
     * @param path the path string
     * @return string represent the format
     */
    public static String formatOfFile(String path) {
        return nameOfExtension(PathUtils.extName(path));
    }

    public static Parser parserForFormat(@NonNull String format) throws UnsupportedFormatException {
        Parser parser = null;
        try {
            parser = parserFor(format);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, e);
        }
        if (parser == null) {
            throw new UnsupportedFormatException(format, "Unsupported format '" + format + "'");
        }
        return parser;
    }

    /**
     * Reads {@code Book} from book file.
     *
     * @param input  book file to be read
     * @param format format of the book file
     * @param args   arguments to parser
     * @return {@code Book} instance represents the book file
     * @throws NullPointerException if the file or format is {@literal null}
     * @throws IOException          if occurs I/O errors
     * @throws JemException         if occurs errors when parsing book file
     */
    public static Book readBook(@NonNull File input, String format, Map<String, Object> args)
            throws IOException, JemException {
        return parserForFormat(format).parse(input, args);
    }

    /**
     * Reads {@code Book} from input path.
     *
     * @param input  path to input
     * @param format format of the book file
     * @param args   arguments to parser
     * @return {@code Book} instance represents the book file
     * @throws NullPointerException if the file or format is {@literal null}
     * @throws IOException          if occurs I/O errors
     * @throws JemException         if occurs errors when parsing book file
     * @since 3.2.0
     */
    public static Book readBook(@NonNull String input, String format, Map<String, Object> args)
            throws IOException, JemException {
        return parserForFormat(format).parse(input, args);
    }

    public static Maker makerForFormat(@NonNull String format) throws UnsupportedFormatException {
        Maker maker = null;
        try {
            maker = makerFor(format);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, e);
        }
        if (maker == null) {
            throw new UnsupportedFormatException(format, "Unsupported format '" + format + "'");
        }
        return maker;
    }

    /**
     * Writes {@code Book} to output file with specified format.
     *
     * @param book   the {@code Book} to be written
     * @param output output book file
     * @param format output format
     * @param args   arguments to maker
     * @throws NullPointerException if the book, output or format is {@literal null}
     * @throws IOException          if occurs I/O errors
     * @throws JemException         if occurs errors when making book file
     */
    public static void writeBook(@NonNull Book book, @NonNull File output, String format, Map<String, Object> args)
            throws IOException, JemException {
        makerForFormat(format).make(book, output, args);
    }

    public static void loadImplementors() {
        val loader = MiscUtils.getContextClassLoader();
        loadRegisters(loader, PARSER_DEFINE_FILE, parsers);
        loadRegisters(loader, MAKER_DEFINE_FILE, makers);
    }

    private static final String NAME_EXTENSION_SEPARATOR = ";";
    private static final String EXTENSION_SEPARATOR = " ";

    private static <T> void loadRegisters(ClassLoader loader, String path, Implementor<T> factory) {
        factory.load(path, loader, new BiFunction<String, String, String>() {
            @Override
            public String apply(String name, String value) {
                val parts = value.split(NAME_EXTENSION_SEPARATOR, 2);
                if (parts.length > 1) {
                    val ext = parts[1].trim();
                    mapExtensions(name, ext.isEmpty() ? setOf(name) : setOf(ext.split(EXTENSION_SEPARATOR)));
                } else {
                    mapExtensions(name, setOf(name));
                }
                return parts[0];
            }
        });
    }

    static {
        if (Boolean.getBoolean(AUTO_LOAD_KEY)) {
            loadImplementors();
        }
    }

}
