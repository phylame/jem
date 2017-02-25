package jem.crawler.impl;

import jem.Chapter;
import jem.crawler.AbstractCrawler;
import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pw.phylame.commons.log.Log;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import static jem.Attributes.*;
import static pw.phylame.commons.util.StringUtils.*;

public class M_ZONGHENG_COM extends AbstractCrawler implements Identifiable {
    private static final String HOST = "http://m.zongheng.com";
    private static final String ENCODING = "UTF-8";
    private static final int PAGE_SIZE = 180;
    private static final String TEXT_JSON_URL = "%s/h5/ajax/chapter?bookId=%s&chapterId=%s";
    private static final String PARAGRAPH_END = "。？”！…※）》】";

    private String bookId;

    @Override
    public void init(CrawlerContext context) {
        super.init(context);
        bookId = valueOfName(secondPartOf(context.getUrl(), "?"), "bookId", "&");
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
        Elements soup = doc.select("div.booksite");
        setCover(book, Flobs.forURL(new URL(soup.select("div.bookimg").select("img").attr("src")), null));
        setTitle(book, soup.select("h1").text().trim());
        soup = soup.select("div.info").first().children();
        setAuthor(book, soup.get(0).child(0).text().trim());
        setGenre(book, soup.get(1).child(0).text().trim());
        setWords(book, Integer.parseInt(soup.get(2).child(0).text().trim()));
        val lines = new LinkedList<String>();
        for (val node : doc.select("div.book_intro").first().childNodes()) {
            if (!(node instanceof TextNode)) {
                continue;
            }
            val str = trimmed(node.toString());
            if (str.isEmpty()) {
                continue;
            }
            lines.add(str);
        }
        setIntro(book, join(context.getConfig().lineSeparator, lines));
        val tags = joinNodes(doc.select("div.tags_wap").first().children(), VALUES_SEPARATOR);
        if (!tags.isEmpty()) {
            setKeywords(book, tags);
        }
    }

    @Override
    public void fetchContents() throws IOException {
        try {
            fetchToc();
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
        }
    }

    @Override
    protected int fetchPage(int page) throws IOException {
        val book = context.getBook();
        final JSONObject json;
        try {
            json = getJson(String.format("%s/h5/ajax/chapter/list?h5=1&bookId=%s&pageNum=%d&pageSize=%d&asc=0", HOST,
                    bookId, page, PAGE_SIZE), ENCODING);
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return 0;
        }
        val list = json.optJSONObject("chapterlist");
        if (list == null) {
            return 0;
        }
        Chapter section = book;
        for (val item : list.getJSONArray("chapters")) {
            val obj = (JSONObject) item;
            val chapter = new Chapter(obj.getString("chapterName"));
            val url = String.format(TEXT_JSON_URL, HOST, bookId, obj.getInt("chapterId"));
            chapter.setText(new CrawlerText(this, chapter, url));
            section.append(chapter);
        }
        if (chapterCount == -1) {
            chapterCount = list.getInt("chapterCount");
            return (int) Math.ceil(chapterCount / list.getDouble("pageSize"));
        } else {
            return 0;
        }
    }

    @Override
    protected String fetchText(String uri) throws IOException {
        ensureInitialized();
        val lines = new LinkedList<String>();
        JSONObject json;
        val b = new StringBuilder();
        while (true) {
            try {
                json = getJson(uri, ENCODING);
            } catch (InterruptedException e) {
                Log.d(TAG, "user interrupted");
                break;
            }
            if (json.getJSONObject("ajaxResult").getInt("code") != 1) {
                break;
            }
            val result = json.getJSONObject("result");
            val parts = result.getString("content").split("</p><p>");
            for (int i = 0, end = parts.length; i != end; ++i) {
                String str = parts[i];
                if (str.isEmpty()) {
                    continue;
                }
                if (i == 0) {
                    str = b.append(str.substring(3)).toString();
                    b.setLength(0);
                    if (str.isEmpty()) {
                        continue;
                    }
                    lines.add(str);
                } else if (i == end - 2 && !PARAGRAPH_END.contains(str.substring(str.length() - 1))) {
                    b.append(str);
                } else if (i != end - 1) { // tip in website
                    lines.add(str);
                }
            }
            if (result.getInt("pageCount") == result.getInt("chapterNum")) {
                break;
            }
            uri = String.format(TEXT_JSON_URL, HOST, bookId, result.getString("nextChapterId"));
        }

        return join(context.getConfig().lineSeparator, lines);
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/h5/book?bookid=%s&h5=1&fpage=180&fmodule=320", HOST, id);
    }

}
