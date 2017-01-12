import java.util.Arrays;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.CrawlerConfig;
import pw.phylame.jem.crawler.OnFetchingListener;
import pw.phylame.jem.crawler.ProviderManager;
import pw.phylame.jem.epm.EpmManager;
import pw.phylame.jem.epm.util.DebugUtils;
import pw.phylame.ycl.util.CollectionUtils;

public class Test {
    public static void main(String[] args) throws Exception {
        System.setProperty(EpmManager.AUTO_LOAD_KEY, "true");
        System.setProperty(ProviderManager.AUTO_LOAD_KEY, "true");
        val url = "http://www.mangg.com/id55123/";
        System.out.println(Arrays.toString(EpmManager.supportedParsers()));
        val book = EpmManager.readBook(url, "crawler", CollectionUtils.<String, Object>mapOf(
                "crawler.parse." + CrawlerConfig.FETCH_LISTENER, new OnFetchingListener() {
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
        DebugUtils.makeFile(book, "E:/tmp/b", "epub", null);
        DebugUtils.makeFile(book, "E:/tmp/b", "pmab", null);
        DebugUtils.makeFile(book, "E:/tmp/b", "umd", null);
        book.cleanup();
        System.out.println("done");
        // val i = new WWW_MANGG_COM();
        // i.init(new CrawlerContext(new Book(), "http://www.mangg.com/id6685/", new CrawlerConfig()));
        // System.out.println(i.fetchText(new Chapter(""), "http://www.mangg.com/id6685/4153685.html"));
    }
}