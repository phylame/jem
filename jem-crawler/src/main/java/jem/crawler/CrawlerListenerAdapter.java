package jem.crawler;

import jem.Book;
import jem.Chapter;

public class CrawlerListenerAdapter implements CrawlerListener {

    @Override
    public void attributeFetched(Book book) {

    }

    @Override
    public void contentsFetched(Book book) {

    }

    @Override
    public void fetchingText(int total, int current, Chapter chapter) {

    }
}
