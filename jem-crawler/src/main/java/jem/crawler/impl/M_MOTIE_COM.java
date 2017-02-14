package jem.crawler.impl;

import static pw.phylame.commons.util.StringUtils.EMPTY_TEXT;
import static pw.phylame.commons.util.StringUtils.secondPartOf;
import static pw.phylame.commons.util.StringUtils.trimmed;

import java.io.IOException;
import java.net.URL;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import jem.core.Attributes;
import jem.core.Chapter;
import jem.crawler.AbstractCrawler;
import jem.crawler.Context;
import jem.crawler.Identifiable;
import jem.crawler.CrawlerText;
import jem.util.flob.Flobs;
import lombok.val;
import pw.phylame.commons.io.PathUtils;

public class M_MOTIE_COM extends AbstractCrawler implements Identifiable {
    private static final String HOST = "http://m.motie.com";
    private static final int PAGE_SIZE = 180;

    private String bookId;
    private Chapter section;

    @Override
    public void init(Context context) {
        super.init(context);
        bookId = PathUtils.baseName(context.getAttrUrl());
        chapterCount = 0;
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
        Elements soup = doc.select("dl.detail_table");
        Attributes.setCover(book, Flobs.forURL(new URL(soup.select("img").attr("src")), null));
        Attributes.setTitle(book, soup.select("h1").text().trim());
        soup = soup.select("p").first().children();
        Attributes.setAuthor(book, soup.get(0).text().trim());
        Attributes.setState(book, soup.get(1).text().trim());
        Attributes.setGenre(book, soup.get(3).text().trim());
        String str = soup.get(7).text().trim();
        str = str.substring(0, str.length() - 1);
        Attributes.setWords(book, Integer.parseInt(str));
        soup = doc.select("div.detail_sum").first().children();
        str = soup.select("div[class=more]").text().trim();
        if (str.isEmpty()) {
            str = trimmed(soup.select("p").text());
        }
        Attributes.setIntro(book, str);
    }

    @Override
    public void fetchContents() throws IOException {
        fetchTocPaged();
    }

    @Override
    protected int fetchPage(int page) throws IOException {
        val doc = getSoup(
                String.format("%s/book/%s/chapter?isAsc=true&page=%d&pageSize=%d", HOST, bookId, page, PAGE_SIZE));
        Elements soup = doc.select("div#bd");
        for (val div : soup.select("div.detail_zx")) {
            String title = div.select("span").text().trim();
            if (title.equals("未分卷")) {
                section = book;
            } else if (section == null || !Attributes.getTitle(section).equals(title)) {
                book.append(section = new Chapter(title));
            }
            for (val li : div.select("li")) {
                val a = li.child(0);
                val chapter = new Chapter(a.text().trim());
                String url;
                if (li.childNodeSize() > 1 && li.child(1).tagName().equals("img")) { // VIP
                    url = EMPTY_TEXT;
                } else {
                    url = a.attr("href");
                }
                chapter.setText(new CrawlerText(url, this, chapter));
                section.append(chapter);
                ++chapterCount;
            }
        }
        val str = doc.select("form.page_form").text().trim();
        return str.isEmpty() ? 0 : Integer.parseInt(secondPartOf(str, "/"));
    }

    @Override
    protected String fetchText(String url) {
        ensureInitialized();
        final Document doc;
        try {
            doc = getSoup(url);
        } catch (IOException e) {
            context.setError(e);
            return EMPTY_TEXT;
        }
        return joinString(doc.select("div.intro").first().children(), config.lineSeparator);
    }

    @Override
    public String attrUrlOf(String id) {
        return String.format("%s/book/%s", HOST, id);
    }
}
