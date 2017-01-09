package pw.phylame.jem.crawler.impl;

import static pw.phylame.ycl.util.StringUtils.EMPTY_TEXT;
import static pw.phylame.ycl.util.StringUtils.join;
import static pw.phylame.ycl.util.StringUtils.secondPartOf;
import static pw.phylame.ycl.util.StringUtils.trimmed;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.Identifiable;
import pw.phylame.jem.crawler.util.HtmlText;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.ycl.util.CollectionUtils;
import pw.phylame.ycl.util.Function;

public class M_QIDIAN_COM extends QIDIAN_COM implements Identifiable {
    private static final String HOST = "http://m.qidian.com";

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
        Elements soup = doc.select("div.book-layout");
        Attributes.setCover(book,
                Flobs.forURL(new URL(largeImage(soup.select("img").attr("src"))), "image/jpg"));
        Attributes.setTitle(book, soup.select("h2").text().trim());
        Attributes.setAuthor(book, soup.select("a").text().trim());
        soup = soup.select("p");
        Attributes.setGenre(book, soup.first().text().trim());
        Attributes.setState(book, secondPartOf(soup.get(1).text(), "|"));
        val lines = new LinkedList<String>();
        for (val str : trimmed(doc.select("section#bookSummary").select("textarea").text()).split("<br>")) {
            lines.add(trimmed(str));
        }
        Attributes.setIntro(book, join(config.lineSeparator, lines));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl() + "/catalog");
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
                chapter.setText(new HtmlText(HOST + a.attr("href"), this, chapter));
                if (section != null) {
                    section.append(chapter);
                } else {
                    book.append(chapter);
                }
                ++total;
            }
        }
        chapterCount = total;
    }

    @Override
    protected String fetchText(String url) {
        ensureInitialized();
        final Document doc;
        try {
            doc = getSoup(url);
        } catch (IOException e) {
            return EMPTY_TEXT;
        }
        return join(config.lineSeparator,
                CollectionUtils.map(doc.select("div.read-section").select("p"), new Function<Element, String>() {
                    @Override
                    public String apply(Element p) {
                        return trimmed(p.text());
                    }
                }));
    }

    @Override
    public String attrUrlOf(String id) {
        return String.format("http://m.qidian.com/book/%s", id);
    }

}
