package pw.phylame.jem.scj.addons;

import java.util.Map;

import pw.phylame.jem.scj.app.SCI;
import pw.phylame.qaf.core.App;
import pw.phylame.qaf.core.Plugin;

public abstract class AbstractPlugin implements Plugin {
    private final Metadata metadata;

    protected final App app = App.INSTANCE;

    protected final SCI sci = SCI.INSTANCE;

    protected AbstractPlugin(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public final String getId() {
        return metadata.getId();
    }

    @Override
    public final Map<String, Object> getMeta() {
        return metadata.toMap();
    }

    @Override
    public void destroy() {

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + getId() + ", meta=" + getMeta() + "]";
    }
}
