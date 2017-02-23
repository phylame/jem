package jem.crawler;

import jem.Chapter;

public class CrawlerListenerAdapter implements CrawlerListener {
    @Override
    public void attributeFetched(CrawlerBook book) {
    }

    @Override
    public void contentsFetched(CrawlerBook book) {
    }

    @Override
    public void textFetching(Chapter chapter, int total, int current) {
    }
}
