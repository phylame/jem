package pw.phylame.jem.crawler;

import org.jsoup.nodes.Document;

import lombok.Data;
import pw.phylame.jem.core.Book;

@Data
public class CrawlerContext {

    private Book book;
    private Document soup;

    private String attrUrl;
    private String tocUrl;

    private CrawlerConfig config;
}
