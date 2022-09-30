package fr.freebuild.playerjoingroup.core.protocol;

public enum Subchannel {
    EVENT("EVENT"),
    QUERY("QUERY"),

    HANDSHAKE("HANDSHAKE"),
    BROADCAST("BROADCAST");

    private String value;

    Subchannel(String param) {
        this.value = param;
    }

    public String getValue() {
        return value;
    }

    public static Subchannel typeof(String value) {
        for (Subchannel type : Subchannel.values()) {
            if (type.getValue().equalsIgnoreCase(value))
                return type;
        }

        throw new IllegalArgumentException("No Subchannel enum with value \"" + value + "\" found.");
    }
}
