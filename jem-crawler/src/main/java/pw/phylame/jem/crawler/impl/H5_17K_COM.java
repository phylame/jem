package pw.phylame.jem.crawler.impl;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.CrawlerConfig;
import pw.phylame.jem.crawler.CrawlerContext;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.value.Pair;

public class H5_17K_COM {
    public Pair<String, String> fetchLinks(String url) {
        return Pair.of(url, null);
    }

    public void fetchAttributes(CrawlerContext ctx) throws IOException {
        val doc = Jsoup.connect(ctx.getAttrUrl()).timeout(3000).get();
        val book = ctx.getBook();
        Elements section = doc.select("section.bookhome_top");
        Attributes.setCover(book, Flobs.forURL(new URL(section.select("img").attr("src")), null));
        Attributes.setTitle(book, section.select("p.title").text());
        Attributes.setAuthor(book, section.select("a.red").text());
        Attributes.setGenre(book, section.select("span").text());
        Attributes.setState(book, section.select("i").get(1).text());
        section = doc.select("section.description");
        val lines = new LinkedList<String>();
        for (val e : section.select("p").first().textNodes()) {
            lines.add(StringUtils.trimmed(e.text()));
        }
        Attributes.setIntro(book, StringUtils.join(System.lineSeparator(), lines));
    }

    public void fetchContents(CrawlerContext ctx) throws IOException {
        String bookId = "1080397";
        int pages = parsePage(ctx, bookId, 1);
        for (int i = 2; i < pages; ++i) {
            parsePage(ctx, bookId, i);
        }
    }

    public int parsePage(CrawlerContext ctx, String id, int page) throws IOException {
        val url = String.format("http://h5.17k.com/h5/book/ajaxBookList.k?bookId=%s&page=%d", id, page);
        val content = IOUtils.toString(IOUtils.openResource(url), "UTF-8");
        val json = new JSONObject(new JSONTokener(content));
        val book = ctx.getBook();
        JSONObject jo;
        String title;
        Chapter section = book, chapter;
        for (val item : json.getJSONArray("datas")) {
            jo = (JSONObject) item;
            title = jo.optString("volumeName");
            if (title != null) { // section
                section.append(chapter = new Chapter(title));
                section = chapter;
            } else {
                section.append(chapter = new Chapter(jo.getString("name")));
                Attributes.setWords(chapter, jo.getInt("wordCount"));
            }
        }
        return json.getInt("totalPage");
    }

    public static void main(String[] args) throws IOException {
        val url = "http://h5.17k.com/book/1080397.html";
        val ctx = new CrawlerContext();
        ctx.setAttrUrl(url);
        ctx.setTocUrl(url.replace("book", "list"));
        ctx.setBook(new Book());
        ctx.setConfig(new CrawlerConfig());
        val impl = new H5_17K_COM();
        impl.fetchAttributes(ctx);
        impl.fetchContents(ctx);
        System.out.println(ctx.getBook());
    }
}
