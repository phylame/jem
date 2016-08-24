package pw.phylame.jem.scj.addons;

import pw.phylame.ycl.util.Linguist;

public final class AddonsMessages {
    public static final String MESSAGES_PATH = "pw/phylame/jem/scj/addons/messages";

    private static final Linguist linguist = new Linguist(MESSAGES_PATH);

    public static String tr(String key) {
        return linguist.tr(key);
    }

    public static String tr(String key, Object... args) {
        return linguist.tr(key, args);
    }
}
