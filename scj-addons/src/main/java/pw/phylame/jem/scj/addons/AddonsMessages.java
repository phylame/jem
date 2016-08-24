package pw.phylame.jem.scj.addons;

import java.util.Locale;

import pw.phylame.qaf.core.Translator;
import pw.phylame.ycl.util.Provider;
import pw.phylame.ycl.value.Lazy;

public final class AddonsMessages {
    public static final String MESSAGES_PATH = "pw/phylame/jem/scj/addons/messages";

    private static final Lazy<Translator> translator = new Lazy<>(new Provider<Translator>() {

        @Override
        public Translator provide() throws Exception {
            return new Translator(MESSAGES_PATH, Locale.getDefault(), Thread.currentThread().getContextClassLoader());
        }

    });

    public static String tr(String key) {
        return translator.get().tr(key);
    }

    public static String tr(String key, Object... args) {
        return translator.get().tr(key, args);
    }
}
