package pw.phylame.jem.scj.addons;

import java.util.Map;
import java.util.UUID;

import lombok.NonNull;
import pw.phylame.jem.scj.app.SCI;
import pw.phylame.qaf.core.Plugin;

public abstract class AbstractPlugin implements Plugin {
    private final Metadata metadata;

    private final String id;

    protected final SCI sci = SCI.INSTANCE;

    protected AbstractPlugin(Metadata metadata) {
        this(metadata, UUID.randomUUID().toString());
    }

    protected AbstractPlugin(@NonNull Metadata metadata, @NonNull String id) {
        this.metadata = metadata;
        this.id = id;
    }

    @Override
    public final String getId() {
        return id;
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
