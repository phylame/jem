package pw.phylame.jem.scj.addons;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.ycl.io.IOUtils;

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
        val in = IOUtils.openResource(app.pathInHome(name + NAME_SUFFIX), null);
        if (in != null) {
            val prop = new Properties();
            prop.load(in);
            for (val e : prop.entrySet()) {
                m.put(e.getKey().toString(), e.getValue());
            }
            in.close();
        }
    }

}
