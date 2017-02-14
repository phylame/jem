import jem.core.Attributes;
import jem.core.Book;
import jem.core.Chapter;
import jem.crawler.CrawlerBook;
import jem.crawler.CrawlerConfig;
import jem.crawler.CrawlerListener;
import jem.crawler.CrawlerManager;
import jem.epm.EpmManager;
import jem.epm.util.DebugUtils;
import lombok.val;
import pw.phylame.commons.util.CollectionUtils;

import java.util.Arrays;

public class Test {
    public static void main(String[] args) throws Exception {
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true");
        System.setProperty(CrawlerManager.AUTO_LOAD_KEY, "true");
        val url = "http://www.mangg.com/id7972/";
        System.out.println(Arrays.toString(EpmManager.supportedParsers()));
        CrawlerBook book = (CrawlerBook) EpmManager.readBook(url, "crawler", CollectionUtils.<String, Object>mapOf(
                "crawler.parse." + CrawlerConfig.CRAWLER_LISTENER, new CrawlerListener() {
                    @Override
                    public void fetchingText(int total, int current, Chapter chapter) {
                        System.out.printf("%d/%d: %s\n", current, total, Attributes.getTitle(chapter));
                    }

                    @Override
                    public void attributeFetched(Book book) {
                    }

                    @Override
                    public void contentsFetched(Book book) {
                    }

                }));
        DebugUtils.printAttributes(book, true);
        // DebugUtils.printTOC(book);
        // System.out.println(book.chapterAt(0).chapterAt(1).getText());
        val start = System.currentTimeMillis();
        book.fetchTexts();
        DebugUtils.makeFile(book, "E:/tmp/b", "pmab", null);
        System.out.println("total: " + (System.currentTimeMillis() - start));
        // val i = new WWW_MANGG_COM();
        // i.init(new Context(new Book(), "http://www.mangg.com/id6685/", new CrawlerConfig()));
        // System.out.println(i.fetchText(new Chapter(""), "http://www.mangg.com/id6685/4153685.html"));
    }
}
