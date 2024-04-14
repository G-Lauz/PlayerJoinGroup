package fr.freebuild.playerjoingroup.spigot;

public interface RetryPolicy {
    boolean shouldRetry(int attempt);
    long getDelay(int attempt);
}
