import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.CrawlerConfig;
import pw.phylame.jem.crawler.OnFetchingListener;
import pw.phylame.jem.crawler.ProviderManager;
import pw.phylame.jem.epm.EpmManager;
import pw.phylame.jem.epm.util.DebugUtils;
import pw.phylame.ycl.util.CollectionUtils;

public class Test {
    public static void main(String[] args) throws Exception {
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true");
        System.setProperty(ProviderManager.AUTO_LOAD_CUSTOMIZED_KEY, "true");
        val url = "http://www.mangg.com/id5812/";
        val book = EpmManager.readBook(url, "crawler", CollectionUtils.<String, Object>mapOf(
                "crawler.parse." + CrawlerConfig.FETCH_LISTENER, new OnFetchingListener() {
                    @Override
                    public void fetching(int total, int current, Chapter chapter) {
                        System.out.printf("%d/%d: %s\n", current, total, Attributes.getTitle(chapter));
                    }
                }
        ));
        DebugUtils.printAttributes(book, true);
        System.out.println(book.chapterAt(20).getText());
    }
}
