package fr.freebuild.playerjoingroup.spigot.utils;

public enum FormatParam {
    PLAYER("{playername}"),
    COUNTER("{counter}");

    private String value;

    FormatParam(String param){
        this.value = param;
    }

    public String getValue() {
        return value;
    }
}
