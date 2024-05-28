package fr.freebuild.playerjoingroup.core.action;

public class ActionExecutionException extends Exception {
    private final boolean commandIsExpired;
    public ActionExecutionException(String message, boolean isExpired) {
        super(message);
        this.commandIsExpired = isExpired;
    }

    public boolean commandIsExpired() {
        return this.commandIsExpired;
    }
}
