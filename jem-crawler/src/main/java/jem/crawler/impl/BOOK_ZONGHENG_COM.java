package jem.crawler.impl;

import static jem.Attributes.VALUES_SEPARATOR;
import static jem.Attributes.setAuthor;
import static jem.Attributes.setCover;
import static jem.Attributes.setGenre;
import static jem.Attributes.setIntro;
import static jem.Attributes.setKeywords;
import static jem.Attributes.setTitle;
import static jem.Attributes.setWords;

import java.io.IOException;
import java.net.URL;

import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import jem.Chapter;
import jem.crawler.AbstractCrawler;
import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import pw.phylame.commons.io.PathUtils;

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
        val doc = getSoup(context.getUrl());
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
        val doc = getSoup(String.format("%s/showchapter/%s.html", HOST, bookId));
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
        return joinNodes(getSoup(uri).select("div#chapterContent>p"), context.getConfig().lineSeparator);
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/book/%s.html", HOST, id);
    }
}
