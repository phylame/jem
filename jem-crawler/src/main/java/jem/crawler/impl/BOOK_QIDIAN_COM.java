package jem.crawler.impl;

import jem.Chapter;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.StringUtils;

import java.io.IOException;
import java.net.URL;

import static jem.Attributes.*;
import static pw.phylame.commons.util.StringUtils.secondPartOf;

public class BOOK_QIDIAN_COM extends QIDIAN_COM implements Identifiable {
    private static final String HOST = "http://book.qidian.com";

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        val config = context.getConfig();
        final Document doc;
        try {
            doc = getSoup(context.getUrl());
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return;
        }
        Elements soup = doc.select("div.book-information");
        setCover(book, Flobs.forURL(new URL(largeImage(soup.select("div.book-img>a>img").attr("src"))), "image/jpg"));
        soup = soup.select("div.book-info");
        setTitle(book, soup.select("h1>em").text().trim());
        setAuthor(book, soup.select("h1 a").text().trim());
        soup = soup.select("p.tag");
        setState(book, soup.select("span").first().text());
        setGenre(book, joinNodes(soup.select("a"), "/"));
        setIntro(book, joinNodes(doc.select("div.book-intro>p:eq(0)").first().childNodes(), config.lineSeparator));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        final Document doc;
        try {
            doc = getSoup(context.getUrl() + "#Catalog");
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return;
        }
        for (val volume : doc.select("div.volume")) {
            val section = new Chapter(secondPartOf(volume.select("h3").text().split("·")[0], " "));
            setWords(section, Integer.parseInt((volume.select("h3 cite").text().trim())));
            book.append(section);
            for (val a : volume.select("ul a")) {
                val chapter = new Chapter(a.text().trim());
                val text = new CrawlerText(this, chapter, protocol + a.attr("href"));
                chapter.setText(text);
                onTextAdded(text);
                section.append(chapter);
            }
        }
    }

    @Override
    protected String fetchText(String uri) throws IOException {
        ensureInitialized();
        try {
            return joinNodes(getSoup(uri).select("div.read-content>p"), context.getConfig().lineSeparator);
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return StringUtils.EMPTY_TEXT;
        }
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/info/%s", HOST, id);
    }
}
