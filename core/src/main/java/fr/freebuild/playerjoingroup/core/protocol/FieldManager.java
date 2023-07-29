package fr.freebuild.playerjoingroup.core.protocol;

import java.util.HashMap;
import java.util.Map;

public class FieldManager {
    private Map<String, String> fields;

    public FieldManager() {
        this.fields = new HashMap<>();
    }

    public void setField(Object key, String value) {
        if (key instanceof ParamsKey)
            this.fields.put(((ParamsKey) key).getValue(), value);
        else if (key instanceof String)
            this.fields.put((String) key, value);
        else
            throw new IllegalArgumentException(
                    "Unsupported key type: \"" + key.getClass().getName() + "\". Expected ParamsKey or String."
            );
    }

    public String get(Object key) {
        if (key instanceof ParamsKey)
            return this.fields.get(((ParamsKey) key).getValue());
        else if (key instanceof String)
            return this.fields.get((String) key);
        else
            throw new IllegalArgumentException(
                    "Unsupported key type: \"" + key.getClass().getName() + "\". Expected ParamsKey or String."
            );
    }

    public Map<String, String> getFields() {
        return this.fields;
    }
}
