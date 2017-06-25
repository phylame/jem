package jem.crawler.impl;

import jem.Chapter;
import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerProvider;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import jclp.io.PathUtils;

import java.io.IOException;
import java.net.URL;

import static jem.Attributes.*;
import static jclp.util.StringUtils.*;

public class M_MOTIE_COM extends CrawlerProvider implements Identifiable {
    private static final String HOST = "http://m.motie.com";
    private static final int PAGE_SIZE = 180;

    private String bookId;
    private Chapter section;

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
        Elements soup = doc.select("dl.detail_table");
        setCover(book, Flobs.forURL(new URL(soup.select("img").attr("src")), null));
        setTitle(book, soup.select("h1").text().trim());
        soup = soup.select("p").first().children();
        setAuthor(book, soup.get(0).text().trim());
        setState(book, soup.get(1).text().trim());
        setGenre(book, soup.get(3).text().trim());
        String str = soup.get(7).text().trim();
        str = str.substring(0, str.length() - 1);
        setWords(book, Integer.parseInt(str));
        soup = doc.select("div.detail_sum").first().children();
        str = soup.select("div[class=more]").text().trim();
        if (str.isEmpty()) {
            str = trimmed(soup.select("p").text());
        }
        setIntro(book, str);
    }

    @Override
    public void fetchContents() throws IOException {
        fetchToc();
    }

    @Override
    protected int fetchPage(int page) throws IOException {
        val book = getContext().getBook();
        final Document doc = getSoup(
                String.format("%s/book/%s/chapter?isAsc=true&page=%d&pageSize=%d", HOST, bookId, page, PAGE_SIZE));
        Elements soup = doc.select("div#bd");
        for (val div : soup.select("div.detail_zx")) {
            String title = div.select("span").text().trim();
            if (title.equals("未分卷")) {
                section = book;
            } else if (section == null || !getTitle(section).equals(title)) {
                book.append(section = new Chapter(title));
            }
            for (val li : div.select("li")) {
                val a = li.child(0);
                val chapter = new Chapter(a.text().trim());
                String url;
                if (li.childNodeSize() > 1 && li.child(1).tagName().equals("img")) { // VIP
                    url = "";
                } else {
                    url = a.attr("href");
                }
                val text = new CrawlerText(this, chapter, url);
                chapter.setText(text);
                section.append(chapter);
                onTextAdded(text);
            }
        }
        val str = doc.select("form.page_form").text().trim();
        return str.isEmpty() ? 0 : Integer.parseInt(secondPartOf(str, "/"));
    }

    @Override
    public String fetchText(String uri) throws IOException {
        ensureInitialized();
        return joinNodes(getSoup(uri).select("div.intro").first().children(), getContext().getConfig().lineSeparator);
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/book/%s", HOST, id);
    }
}
