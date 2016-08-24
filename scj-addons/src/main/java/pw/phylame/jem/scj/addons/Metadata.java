package pw.phylame.jem.scj.addons;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Value;
import lombok.val;

@Value
public class Metadata {
    private String name;
    private String version;
    private String vendor;

    public Map<String, Object> toMap() {
        val map = new LinkedHashMap<String, Object>();
        map.put("name", name);
        map.put("version", version);
        map.put("vendor", vendor);
        return map;
    }
}
