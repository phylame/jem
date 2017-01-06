package pw.phylame.jem.crawler;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.impl.AbstractParser;
import pw.phylame.jem.epm.util.ParserException;

public class CrawlerParser extends AbstractParser<Closeable, CrawlerConfig> {

    public CrawlerParser() {
        super("crawler", CrawlerConfig.class);
    }

    @Override
    protected Closeable openInput(File file, CrawlerConfig config) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Book parse(Closeable input, CrawlerConfig config) throws IOException, ParserException {
        // TODO Auto-generated method stub
        return null;
    }

}
