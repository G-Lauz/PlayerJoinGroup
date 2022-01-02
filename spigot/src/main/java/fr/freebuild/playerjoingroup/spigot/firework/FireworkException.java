package fr.freebuild.playerjoingroup.spigot.firework;

public class FireworkException extends Exception {
    private static final long serialVersionUID = 1L;

    public FireworkException(String message) {
        super("[Firework] Disabled. Caused by: " + message);
    }
}
