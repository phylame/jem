package jem.crawler.impl;

import jem.Chapter;
import jem.crawler.AbstractCrawler;
import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pw.phylame.commons.io.PathUtils;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.StringUtils;

import java.io.IOException;
import java.net.URL;

import static jem.Attributes.*;

public class BOOK_ZONGHENG_COM extends AbstractCrawler implements Identifiable {
    private static final String HOST = "http://book.zongheng.com";

    private String bookId;

    @Override
    public void init(CrawlerContext context) {
        super.init(context);
        bookId = PathUtils.baseName(context.getUrl());
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        final Document doc;
        try {
            doc = getSoup(context.getUrl());
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return;
        }
        Elements soup = doc.select("div.main");
        setCover(book, Flobs.forURL(new URL(soup.select("div.book_cover img").attr("src")), null));
        soup = soup.select("div.status");
        setTitle(book, soup.select("h1>a").text().trim());
        setIntro(book, joinNodes(soup.select("div.info_con>p"), context.getConfig().lineSeparator));
        setKeywords(book, joinNodes(soup.select("div.keyword a"), VALUES_SEPARATOR));
        soup = soup.select("div.booksub");
        setAuthor(book, soup.select("a:eq(1)").text().trim());
        setGenre(book, soup.select("a:eq(3)").text().trim());
        setWords(book, Integer.parseInt(soup.select("span").text().trim()));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        chapterCount = 0;
        final Document doc;
        try {
            doc = getSoup(String.format("%s/showchapter/%s.html", HOST, bookId));
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return;
        }
        val top = doc.select("div#chapterListPanel");
        val i1 = top.select("h5").iterator();
        val i2 = top.select("div.booklist").iterator();
        while (i1.hasNext() && i2.hasNext()) {
            val section = new Chapter(((TextNode) i1.next().childNode(0)).text().trim());
            for (val a : i2.next().select("a")) {
                val chapter = new Chapter(a.text().trim());
                chapter.setText(new CrawlerText(this, chapter, a.attr("href")));
                section.append(chapter);
                ++chapterCount;
            }
            book.append(section);
        }
    }

    @Override
    protected String fetchText(String uri) throws IOException {
        ensureInitialized();
        try {
            return joinNodes(getSoup(uri).select("div#chapterContent>p"), context.getConfig().lineSeparator);
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return StringUtils.EMPTY_TEXT;
        }
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/book/%s.html", HOST, id);
    }
}
