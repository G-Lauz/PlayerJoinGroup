package fr.freebuild.playerjoingroup.core.protocol;

public enum QueryType {
    HAS_PLAYED_BEFORE("HasPlayedBefore"),
    HAS_PLAYED_BEFORE_RESPONSE("ResponseHasPlayedBefore");

    private String value;

    QueryType(String param) {
        this.value = param;
    }

    public String getValue() {
        return value;
    }

    public static QueryType typeof(String value) {
        for (QueryType type : QueryType.values()) {
            if (type.getValue().equalsIgnoreCase(value))
                return type;
        }

        throw new IllegalArgumentException("No QueryType enum with value \"" + value + "\" found.");
    }
}
