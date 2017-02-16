package jem.crawler.impl;

import jem.Attributes;
import jem.Chapter;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import static pw.phylame.commons.util.StringUtils.*;

public class BOOK_QIDIAN_COM extends QIDIAN_COM implements Identifiable {
    private static final String HOST = "http://book.qidian.com";

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
        Elements soup = doc.select("div.book-information");
        Attributes.setCover(book,
                Flobs.forURL(new URL(largeImage(soup.select("div.book-img>a>img").attr("src"))), "image/jpg"));
        soup = soup.select("div.book-info");
        Attributes.setTitle(book, soup.select("h1>em").text().trim());
        Attributes.setAuthor(book, soup.select("h1 a").text().trim());
        soup = soup.select("p.tag");
        Attributes.setState(book, soup.select("span").first().text());
        val genres = new LinkedList<String>();
        for (val a : soup.select("a")) {
            genres.add(a.text().trim());
        }
        Attributes.setGenre(book, join("/", genres));
        val lines = new LinkedList<String>();
        for (val node : doc.select("div.book-intro>p:eq(0)").first().childNodes()) {
            if (!(node instanceof TextNode)) {
                continue;
            }
            lines.add(trimmed(node.toString()));
        }
        Attributes.setIntro(book, join(config.lineSeparator, lines));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        chapterCount = 0;
        val doc = getSoup(context.getAttrUrl() + "#Catalog");
        for (val volume : doc.select("div.volume")) {
            val section = new Chapter(secondPartOf(volume.select("h3").text().split("·")[0], " "));
            Attributes.setWords(section, Integer.parseInt((volume.select("h3 cite").text().trim())));
            book.append(section);
            for (val a : volume.select("ul a")) {
                val chapter = new Chapter(a.text().trim());
                chapter.setText(new CrawlerText(protocol + a.attr("href"), this, chapter));
                section.append(chapter);
                ++chapterCount;
            }
        }
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
        return joinString(doc.select("div.read-content>p"), config.lineSeparator);
    }

    @Override
    public String attrUrlOf(String id) {
        return String.format("%s/info/%s", HOST, id);
    }

}
