package pw.phylame.jem.scj.addons;

import java.io.IOException;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.ycl.util.MiscUtils;

public class EpmArgumentsLoader extends AbstractPlugin {

    private static final String NAME_SUFFIX = ".prop";

    public EpmArgumentsLoader() {
        super(new Metadata("ee4ef607-a500-4e16-aecc-08aa457a60ea", "Epm Arguments Loader", "1.0", "PW"));
    }

    @Override
    public void init() {
        val cfg = AppConfig.INSTANCE;
        update("in-args", cfg.getInArguments());
        update("out-attrs", cfg.getOutAttributes());
        update("out-exts", cfg.getOutExtensions());
        update("out-args", cfg.getOutArguments());
    }

    @SneakyThrows(IOException.class)
    private void update(String name, Map<String, Object> m) {
        val prop = MiscUtils.propertiesFor(app.pathOf(name + NAME_SUFFIX), getClass().getClassLoader());
        if (prop != null) {
            for (val e : prop.entrySet()) {
                m.put(e.getKey().toString(), e.getValue());
            }
        }
    }
}
