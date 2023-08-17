package fr.freebuild.playerjoingroup.spigot;

public class CommandExecutionException extends Exception {
    private final boolean commandIsExpired;
    public CommandExecutionException(String message, boolean isExpired) {
        super(message);
        this.commandIsExpired = isExpired;
    }

    public boolean commandIsExpired() {
        return this.commandIsExpired;
    }
}
