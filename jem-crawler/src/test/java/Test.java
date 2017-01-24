import java.util.Arrays;

import jem.core.Attributes;
import jem.core.Book;
import jem.core.Chapter;
import jem.crawler.CrawlerConfig;
import jem.crawler.OnFetchingListener;
import jem.crawler.ProviderManager;
import jem.epm.EpmManager;
import jem.epm.util.DebugUtils;
import lombok.val;
import pw.phylame.commons.util.CollectionUtils;

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
