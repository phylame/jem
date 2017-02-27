package jem.crawler.impl;

import jem.Chapter;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import static jem.Attributes.*;
import static pw.phylame.commons.util.StringUtils.*;

public class M_QIDIAN_COM extends QIDIAN_COM implements Identifiable {
    private static final String HOST = "http://m.qidian.com";

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
        Elements soup = doc.select("div.book-layout");
        setCover(book, Flobs.forURL(new URL(largeImage(soup.select("img").attr("src"))), "image/jpg"));
        setTitle(book, soup.select("h2").text().trim());
        setAuthor(book, soup.select("a").text().trim());
        soup = soup.select("p");
        setGenre(book, soup.first().text().trim());
        setState(book, secondPartOf(soup.get(1).text(), "|"));
        val lines = new LinkedList<String>();
        for (val str : trimmed(doc.select("section#bookSummary").select("textarea").text()).split("<br>")) {
            lines.add(trimmed(str));
        }
        setIntro(book, join(context.getConfig().lineSeparator, lines));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        final Document doc;
        try {
            doc = getSoup(context.getUrl() + "/catalog");
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return;
        }
        Chapter section = null;
        int total = 0;
        for (val node : doc.select("ol#volumes").first().childNodes()) {
            if (!(node instanceof Element)) {
                continue;
            }
            val li = (Element) node;
            if (!li.tagName().equalsIgnoreCase("li")) {
                continue;
            }
            if (li.attr("class").equals("chapter-bar")) { // section
                book.append(section = new Chapter(li.text().trim()));
            } else {
                val a = li.child(0);
                val chapter = new Chapter(a.child(0).text().trim());
                chapter.setText(new CrawlerText(this, chapter, HOST + a.attr("href")));
                if (section != null) {
                    section.append(chapter);
                } else {
                    book.append(chapter);
                }
                ++total;
            }
        }
        chapterCount = total;
        book.setTotalChapters(total);
    }

    @Override
    protected String fetchText(String uri) throws IOException {
        ensureInitialized();
        try {
            return joinNodes(getSoup(uri).select("div.read-section").select("p"), context.getConfig().lineSeparator);
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return StringUtils.EMPTY_TEXT;
        }
    }

    @Override
    public String urlById(String id) {
        return String.format("http://m.qidian.com/book/%s", id);
    }

}
