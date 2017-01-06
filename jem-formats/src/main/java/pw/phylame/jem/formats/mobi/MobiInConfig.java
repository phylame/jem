package pw.phylame.jem.formats.mobi;

import pw.phylame.jem.epm.util.config.AbstractConfig;

public class MobiInConfig extends AbstractConfig {
    public static final String SELF = "config";

    public String textEncoding;
    public int maxHeaderLength=500;
    public boolean fixExtraData = false;
}
