package jem.crawler.util;

import pw.phylame.commons.io.TextCache;

import java.io.File;
import java.io.IOException;

public class FileCache implements Cacheable {
    private final TextCache cache;

    public FileCache(File cache) {
        this.cache = new TextCache(cache);
    }

    @Override
    public Object add(String text) {
        return cache.add(text);
    }

    @Override
    public String get(Object tag) {
        return cache.get(tag);
    }

    @Override
    public void close() throws IOException {
        cache.close();
    }
}
