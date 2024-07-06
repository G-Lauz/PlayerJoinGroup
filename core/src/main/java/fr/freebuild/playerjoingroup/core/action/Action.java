package fr.freebuild.playerjoingroup.core.action;

public abstract class Action<T> {
    private final long timeout;
    private final long creationTime;

    public Action(long timeout) {
        this.timeout = timeout;
        this.creationTime = System.currentTimeMillis();
    }

    public abstract void execute(T context);
    public abstract int hashCode();

    public boolean isExpired() {
        return System.currentTimeMillis() - this.creationTime > this.timeout;
    }
}
