package jem.crawler.impl;

import jem.Chapter;
import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerProvider;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import jclp.io.PathUtils;

import java.io.IOException;
import java.net.URL;

import static jem.Attributes.*;

public class BOOK_ZONGHENG_COM extends CrawlerProvider implements Identifiable {
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
        val context = getContext();
        val book = context.getBook();
        final Document doc = getSoup(context.getUrl());
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
        val book = getContext().getBook();
        final Document doc = getSoup(String.format("%s/showchapter/%s.html", HOST, bookId));
        val top = doc.select("div#chapterListPanel");
        val i1 = top.select("h5").iterator();
        val i2 = top.select("div.booklist").iterator();
        while (i1.hasNext() && i2.hasNext()) {
            val section = new Chapter(((TextNode) i1.next().childNode(0)).text().trim());
            for (val a : i2.next().select("a")) {
                val chapter = new Chapter(a.text().trim());
                val text = new CrawlerText(this, chapter, a.attr("href"));
                chapter.setText(text);
                onTextAdded(text);
                section.append(chapter);
            }
            book.append(section);
        }
    }

    @Override
    public String fetchText(String uri) throws IOException {
        ensureInitialized();
        return joinNodes(getSoup(uri).select("div#chapterContent>p"), getContext().getConfig().lineSeparator);
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/book/%s.html", HOST, id);
    }
}
