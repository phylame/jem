package pw.phylame.jem.crawler.impl;

import java.io.IOException;
import java.net.URL;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.AbstractProvider;
import pw.phylame.jem.crawler.CrawlerContext;
import pw.phylame.jem.crawler.Identifiable;
import pw.phylame.jem.crawler.util.HtmlText;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.util.StringUtils;

public class BOOK_ZONGHENG_COM extends AbstractProvider implements Identifiable {
    private static final String HOST = "http://book.zongheng.com";

    private String bookId;

    @Override
    public void init(CrawlerContext context) {
        super.init(context);
        bookId = PathUtils.baseName(context.getAttrUrl());
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
        Elements soup = doc.select("div.main");
        Attributes.setCover(book, Flobs.forURL(new URL(soup.select("div.book_cover img").attr("src")), null));
        soup = soup.select("div.status");
        Attributes.setTitle(book, soup.select("h1>a").text().trim());
        Attributes.setIntro(book, joinString(soup.select("div.info_con>p"), config.lineSeparator));
        Attributes.setKeywords(book, joinString(soup.select("div.keyword a"), Attributes.VALUES_SEPARATOR));
        soup = soup.select("div.booksub");
        Attributes.setAuthor(book, soup.select("a:eq(1)").text().trim());
        Attributes.setGenre(book, soup.select("a:eq(3)").text().trim());
        Attributes.setWords(book, Integer.parseInt(soup.select("span").text().trim()));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        chapterCount = 0;
        val doc = getSoup(String.format("%s/showchapter/%s.html", HOST, bookId));
        val top = doc.select("div#chapterListPanel");
        val i1 = top.select("h5").iterator();
        val i2 = top.select("div.booklist").iterator();
        while (i1.hasNext() && i2.hasNext()) {
            val section = new Chapter(((TextNode) i1.next().childNode(0)).text().trim());
            for (val a : i2.next().select("a")) {
                val chapter = new Chapter(a.text().trim());
                chapter.setText(new HtmlText(a.attr("href"), this, chapter));
                section.append(chapter);
                ++chapterCount;
            }
            book.append(section);
        }

    }

    @Override
    protected String fetchText(String url) {
        ensureInitialized();
        final Document doc;
        try {
            doc = getSoup(url);
        } catch (IOException e) {
            return StringUtils.EMPTY_TEXT;
        }
        return joinString(doc.select("div#chapterContent>p"), config.lineSeparator);
    }

    @Override
    public String attrUrlOf(String id) {
        return String.format("%s/book/%s.html", HOST, id);
    }
}
