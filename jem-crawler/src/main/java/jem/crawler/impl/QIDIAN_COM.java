package jem.crawler.impl;

import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerProvider;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class QIDIAN_COM extends CrawlerProvider {

    protected String protocol;

    @Override
    public void init(CrawlerContext context) {
        super.init(context);
        try {
            protocol = new URL(context.getUrl()).getProtocol() + ':';
        } catch (MalformedURLException e) {
            protocol = "http:";
        }
    }

    protected final String largeImage(String url) {
        return protocol + url.replaceFirst("[\\d]+$", "600");
    }
}
