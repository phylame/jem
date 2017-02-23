package jem.crawler.util;

import java.io.Closeable;

public interface Cacheable extends Closeable {
    Object add(String text);

    String get(Object tag);
}
