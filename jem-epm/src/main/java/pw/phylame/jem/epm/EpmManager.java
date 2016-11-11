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

package pw.phylame.jem.epm;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.util.JemException;
import pw.phylame.jem.util.UnsupportedFormatException;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.CollectUtils;
import pw.phylame.ycl.util.Implementor;

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
     * Name of system property to auto load customized parsers and parsers.
     */
    public static final String AUTO_LOAD_CUSTOMIZED_KEY = "jem.emp.autoLoad";

    /**
     * Holds registered {@code Parser} class information.
     */
    private static final Implementor<Parser> parsers = new Implementor<>(Parser.class, true, null);

    /**
     * Holds registered {@code Maker} class information.
     */
    private static final Implementor<Maker> makers = new Implementor<>(Maker.class, true, null);

    /**
     * Mapping parser and maker name to file extension names.
     */
    private static final Map<String, Set<String>> extensions = new ConcurrentHashMap<>();

    /**
     * Mapping file extension name to parser and maker name.
     */
    private static final Map<String, String> names = new ConcurrentHashMap<>();

    /**
     * Registers parser class with specified name.
     * <p>
     * If parser class with same name exists, replaces the old with the new parser class.
     * </p>
     * <p>
     * NOTE: old parser and cached parser with the name will be removed.
     * </p>
     *
     * @param name
     *            name of the parser (normally the extension name of book file)
     * @param path
     *            path of the parser class
     * @throws IllegalArgumentException
     *             if the <code>name</code> or <code>path</code> is {@literal null} or empty string
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
     * @param name
     *            name of the parser (normally the extension name of book file)
     * @param clazz
     *            the {@code Parser} class
     * @throws IllegalArgumentException
     *             if the <code>name</code> is {@literal null} or empty string
     * @throws NullPointerException
     *             if the <code>clazz</code> is {@literal null}
     */
    public static void registerParser(String name, Class<? extends Parser> clazz) {
        parsers.register(name, clazz);
    }

    /**
     * Removes registered parser with specified name.
     *
     * @param name
     *            name of the parser
     */
    public static void removeParser(String name) {
        parsers.remove(name);
    }

    /**
     * Tests parser with specified name is registered or not.
     *
     * @param name
     *            the name of format
     * @return <code>true</code> if the parser is registered otherwise <code>false</code>
     */
    public static boolean hasParser(String name) {
        return parsers.contains(name);
    }

    /**
     * Returns names of registered parser class.
     *
     * @return sequence of format names
     */
    public static String[] supportedParsers() {
        return parsers.names();
    }

    /**
     * Returns parser instance with specified name.
     *
     * @param name
     *            name of the parser
     * @return {@code Parser} instance or {@literal null} if parser not registered
     * @throws NullPointerException
     *             if the <code>name</code> is {@literal null}
     * @throws IllegalAccessException
     *             cannot access the parser class
     * @throws InstantiationException
     *             cannot create new instance of parser class
     * @throws ClassNotFoundException
     *             if registered class path is invalid
     */
    public static Parser parserFor(String name)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
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
     * @param name
     *            name of the maker (normally the extension name of book file)
     * @param path
     *            class path of the maker class
     * @throws IllegalArgumentException
     *             if the <code>name</code> or <code>path</code> is {@literal null} or empty string
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
     * @param name
     *            name of the maker (normally the extension name of book file)
     * @param clazz
     *            the {@code Maker} class
     * @throws IllegalArgumentException
     *             if the <code>name</code> is {@literal null} or empty string
     * @throws NullPointerException
     *             if the <code>clazz</code> is {@literal null}
     */
    public static void registerMaker(String name, Class<? extends Maker> clazz) {
        makers.register(name, clazz);
    }

    /**
     * Removes registered maker with specified name.
     *
     * @param name
     *            name of the maker
     */
    public static void removeMaker(String name) {
        makers.remove(name);
    }

    /**
     * Tests maker with specified name is registered or not.
     *
     * @param name
     *            the name of format
     * @return <code>true</code> if the maker is registered otherwise <code>false</code>
     */
    public static boolean hasMaker(String name) {
        return makers.contains(name);
    }

    /**
     * Returns names of registered maker class.
     *
     * @return sequence of format names
     */
    public static String[] supportedMakers() {
        return makers.names();
    }

    /**
     * Returns maker instance with specified name.
     *
     * @param name
     *            name of the maker
     * @return {@code Maker} instance or {@literal null} if maker not registered
     * @throws NullPointerException
     *             if the <code>name</code> is {@literal null}
     * @throws IllegalAccessException
     *             cannot access the maker class
     * @throws InstantiationException
     *             cannot create new instance of maker class
     * @throws ClassNotFoundException
     *             if registered class path is invalid
     */
    public static Maker makerFor(String name)
            throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        return makers.getInstance(name);
    }

    /**
     * Maps specified file extension names to parser (or maker) name.
     *
     * @param name
     *            the name of parser or maker
     * @param extensions
     *            file extension names supported by the parser (or maker), if {@literal null} use the parser name as
     *            one extension
     * @throws NullPointerException
     *             if the <code>name</code> is {@literal null}
     */
    public static void mapExtensions(@NonNull String name, Collection<String> extensions) {
        Set<String> current = EpmManager.extensions.get(name);
        if (current == null) {
            EpmManager.extensions.put(name, current = new HashSet<>());
        }
        if (CollectUtils.isEmpty(extensions)) {
            current.add(name);
        } else {
            current.addAll(extensions);
        }
        for (val ext : current) {
            names.put(ext, name);
        }
    }

    /**
     * Gets supported file extension names of specified parser or maker name.
     *
     * @param name
     *            the name of parser or maker
     * @return the string set of extension name
     */
    public static String[] extensionsOfName(String name) {
        val result = extensions.get(name);
        return result.toArray(new String[result.size()]);
    }

    /**
     * Gets parser or maker name by file extension name.
     *
     * @param extension
     *            the extension name
     * @return the name or {@literal null} if the extension name is unknown.
     */
    public static String nameOfExtension(String extension) {
        return names.get(extension);
    }

    /**
     * Gets the format of specified file path.
     *
     * @param path
     *            the path string
     * @return string represent the format
     */
    public static String formatOfFile(String path) {
        return nameOfExtension(PathUtils.extensionName(path).toLowerCase());
    }

    public static Parser parserForFormat(@NonNull String format) throws UnsupportedFormatException {
        Parser parser = null;
        try {
            parser = parserFor(format);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            Log.e(TAG, e);
        }
        if (parser == null) {
            throw new UnsupportedFormatException(format, "Unsupported format '" + format + '\'');
        }
        return parser;
    }

    /**
     * Reads {@code Book} from book file.
     *
     * @param input
     *            book file to be read
     * @param format
     *            format of the book file
     * @param arguments
     *            arguments to parser
     * @return {@code Book} instance represents the book file
     * @throws NullPointerException
     *             if the file or format is {@literal null}
     * @throws IOException
     *             if occurs I/O errors
     * @throws JemException
     *             if occurs errors when parsing book file
     */
    public static Book readBook(@NonNull File input, String format, Map<String, Object> arguments)
            throws IOException, JemException {
        return parserForFormat(format).parse(input, arguments);
    }

    public static Maker makerForFormat(@NonNull String format) throws UnsupportedFormatException {
        Maker maker = null;
        try {
            maker = makerFor(format);
        } catch (IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            Log.e(TAG, e);
        }
        if (maker == null) {
            throw new UnsupportedFormatException(format, "Unsupported format '" + format + "'");
        }
        return maker;
    }

    /**
     * Writes {@code Book} to book with specified format.
     *
     * @param book
     *            the {@code Book} to be written
     * @param output
     *            output book file
     * @param format
     *            output format
     * @param arguments
     *            arguments to maker
     * @throws NullPointerException
     *             if the book, output or format is {@literal null}
     * @throws IOException
     *             if occurs I/O errors
     * @throws JemException
     *             if occurs errors when making book file
     */
    public static void writeBook(@NonNull Book book, @NonNull File output, String format, Map<String, Object> arguments)
            throws IOException, JemException {
        makerForFormat(format).make(book, output, arguments);
    }

    public static void loadCustomizedImplementors() {
        val loader = Thread.currentThread().getContextClassLoader();
        loadRegisters(loader, PARSER_DEFINE_FILE, parsers);
        loadRegisters(loader, MAKER_DEFINE_FILE, makers);
    }

    private static final String NAME_EXTENSION_SEPARATOR = ";";
    private static final String EXTENSION_SEPARATOR = " ";

    private static <T> void loadRegisters(ClassLoader loader, String path, Implementor<T> factory) {
        val urls = IOUtils.resourcesFor(path, loader);
        if (urls == null) {
            return;
        }
        while (urls.hasMoreElements()) {
            try (val in = urls.nextElement().openStream()) {
                val prop = new Properties();
                prop.load(in);
                for (val e : prop.entrySet()) {
                    String name = e.getKey().toString();
                    String[] parts = e.getValue().toString().split(NAME_EXTENSION_SEPARATOR, 2);
                    factory.register(name, parts[0]);
                    if (parts.length > 1) {
                        mapExtensions(name, CollectUtils.setOf(parts[1].toLowerCase().split(EXTENSION_SEPARATOR)));
                    } else {
                        mapExtensions(name, null);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, e);
            }
        }
    }

    static {
        if (Boolean.getBoolean(AUTO_LOAD_CUSTOMIZED_KEY)) {
            loadCustomizedImplementors();
        }
    }

}
