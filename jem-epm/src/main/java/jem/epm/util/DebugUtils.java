/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.epm.util;

import jclp.io.PathUtils;
import jclp.util.StringUtils;
import jem.Book;
import jem.Chapter;
import jem.epm.EpmManager;
import jem.util.JemException;
import jem.util.Variants;
import lombok.val;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static jem.Attributes.getTitle;
import static jem.Attributes.titleOf;

/**
 * Utilities for debug parser and maker.
 */
public final class DebugUtils {
    private DebugUtils() {
    }

    public static Book parseFile(String path, Map<String, Object> args) {
        val format = EpmManager.nameOfExtension(PathUtils.extName(path));
        if (format == null) {
            System.err.println("unsupported format: " + path);
            return null;
        }
        return parseFile(path, format, args);
    }

    public static Book parseFile(String path, String format, Map<String, Object> args) {
        Book book = null;
        try {
            book = EpmManager.readBook(new File(path), format, args);
        } catch (IOException | JemException e) {
            e.printStackTrace();
        }
        return book;
    }

    public static boolean makeFile(Book book, String path, Map<String, Object> args) {
        val format = EpmManager.nameOfExtension(PathUtils.extName(path));
        if (format == null) {
            System.err.println("unsupported format: " + path);
            return false;
        }
        return makeFile(book, path, format, args);
    }

    public static boolean makeFile(Book book, String path, String format, Map<String, Object> args) {
        File file = new File(path);
        if (file.isDirectory()) {
            file = new File(file, getTitle(book) + "." + format);
        }
        try {
            EpmManager.writeBook(book, file, format, args);
        } catch (IOException | JemException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void printAttributes(Chapter chapter, boolean showLine) {
        if (showLine) {
            System.out.println(StringUtils.duplicated("-", 36));
        }
        System.out.println(chapter.getAttributes().size() + " attributes of " + getTitle(chapter));
        for (val e : chapter.getAttributes().entries()) {
            String title = titleOf(e.getKey());
            if (title == null) {
                title = StringUtils.capitalized(e.getKey());
            }
            System.out.printf(" o. %s(%s)=%s\n", title, e.getKey(), Variants.printable(e.getValue()));
        }
    }

    public static void printExtension(Book book, boolean showLine) {
        if (showLine) {
            System.out.println(StringUtils.duplicated("-", 36));
        }
        System.out.println(book.getExtensions().size() + " extensions of " + getTitle(book));
        for (val e : book.getExtensions().entries()) {
            System.out.printf(" o. %s=%s\n", e.getKey(), Variants.printable(e.getValue()));
        }
    }

    public static void printTOC(Chapter chapter) {
        printTOC(chapter, "", " ");
    }

    public static void printTOC(Chapter chapter, String prefix) {
        printTOC(chapter, prefix, " ");
    }

    public static void printTOC(Chapter chapter, String prefix, String separator) {
        System.out.print(prefix);
        printAttributes(chapter, false);
        int order = 1;
        for (val sub : chapter) {
            printTOC(sub, prefix + order + separator, separator);
            ++order;
        }
    }

}
