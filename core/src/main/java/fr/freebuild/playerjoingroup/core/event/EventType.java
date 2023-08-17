package fr.freebuild.playerjoingroup.core.event;

public enum EventType {
    LEAVE_SERVER_GROUP("LeaveServerGroup"),
    FIRST_GROUP_CONNECTION("FirstGroupConnection"),
    HAS_PLAYED_BEFORE("HasPlayedBefore"),
    SERVER_CONNECT("ServerConnect"),
    SERVER_DISCONNECT("ServerDisconnect");

    private String value;

    EventType(String param) {
        this.value = param;
    }

    public String getValue() {
        return value;
    }

    public static EventType typeof(String value) {
        for (EventType type : EventType.values()) {
            if (type.getValue().equalsIgnoreCase(value))
                return type;
        }

        throw new IllegalArgumentException("No EventType enum with value \"" + value + "\" found.");
    }
}
