package fr.freebuild.playerjoingroup.core.protocol;

public class UnknownGroupException extends Exception{
    public UnknownGroupException(String group) {
        super("Unknown group: " + group);
    }
}
