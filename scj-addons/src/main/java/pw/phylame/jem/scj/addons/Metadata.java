package pw.phylame.jem.scj.addons;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.NonNull;
import lombok.Value;
import lombok.val;

@Value
public class Metadata {
    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private String version;
    @NonNull
    private String vendor;

    public Map<String, Object> toMap() {
        val map = new LinkedHashMap<String, Object>();
        map.put("name", name);
        map.put("version", version);
        map.put("vendor", vendor);
        return map;
    }
}
