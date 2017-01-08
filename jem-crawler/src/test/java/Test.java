import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.crawler.CrawlerConfig;
import pw.phylame.jem.crawler.CrawlerContext;
import pw.phylame.jem.crawler.ProviderManager;
import pw.phylame.jem.epm.EpmManager;

public class Test {
    public static void main(String[] args) throws Exception {
        System.setProperty(EpmManager.AUTO_LOAD_CUSTOMIZED_KEY, "true");
        System.setProperty(ProviderManager.AUTO_LOAD_CUSTOMIZED_KEY, "true");
        val provider = ProviderManager.providerForHost("https://yd.sogou.com");
        val url = ProviderManager.getAttrUrlById(provider, "7CF8A4E9AC33064A95ABF2848B56FDBD");
        val content = new CrawlerContext(new Book(), url, new CrawlerConfig());
        provider.init(content);
        provider.fetchContents();
        provider.fetchAttributes();
    }
}
