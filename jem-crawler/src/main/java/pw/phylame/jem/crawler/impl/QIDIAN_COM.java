package pw.phylame.jem.crawler.impl;

import java.net.MalformedURLException;
import java.net.URL;

import pw.phylame.jem.crawler.AbstractProvider;
import pw.phylame.jem.crawler.CrawlerContext;

public abstract class QIDIAN_COM extends AbstractProvider {

    protected String protocol;

    @Override
    public void init(CrawlerContext context) {
        super.init(context);
        try {
            protocol = new URL(context.getAttrUrl()).getProtocol() + ':';
        } catch (MalformedURLException e) {
            protocol = "http:";
        }
    }

    protected final String largeImage(String url) {
        return protocol + url.replaceFirst("[\\d]+$", "600");
    }
}
