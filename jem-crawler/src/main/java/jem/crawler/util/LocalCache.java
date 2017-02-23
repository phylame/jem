package jem.crawler.util;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class LocalCache implements Cacheable {
    private StringBuilder b = new StringBuilder();
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Object add(String text) {
        lock.lock();
        Tag tag;
        try {
            tag = new Tag(b.length(), text.length());
            b.append(text);
        } finally {
            lock.unlock();
        }
        return tag;
    }

    @Override
    public String get(Object tag) {
        if (tag instanceof Tag) {
            lock.lock();
            try {
                val id = (Tag) tag;
                return b.substring(id.offset, id.length);
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        b = null;
        lock.unlock();
    }

    @ToString
    @RequiredArgsConstructor
    private static class Tag {
        private final int offset;
        private final int length;
    }
}
