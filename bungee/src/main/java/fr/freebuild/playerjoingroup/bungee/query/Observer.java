package fr.freebuild.playerjoingroup.bungee.query;

public interface Observer<T> {
    public void notify(T obj);
}
