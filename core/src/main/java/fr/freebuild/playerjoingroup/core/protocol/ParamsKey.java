package fr.freebuild.playerjoingroup.core.protocol;

public enum ParamsKey {
    SERVER_GROUP("serverGroup"),
    EVENT("event"),
    QUERY("query"),
    HASH_CODE("hashCode");

    private String value;

    ParamsKey(String param) {
        this.value = param;
    }

    public String getValue() {
        return value;
    }

    public static ParamsKey typeof(String value) {
        for (ParamsKey type : ParamsKey.values()) {
            if (type.getValue().equalsIgnoreCase(value))
                return type;
        }

        throw new IllegalArgumentException("No ParamsKey enum with value \"" + value + "\" found.");
    }
}
