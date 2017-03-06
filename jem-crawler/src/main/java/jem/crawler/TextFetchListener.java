package jem.crawler;

import jem.Chapter;

public interface TextFetchListener {
    void textFetched(Chapter chapter, int total, int progress);
}
