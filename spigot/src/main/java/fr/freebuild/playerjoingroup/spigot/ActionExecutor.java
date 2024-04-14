package fr.freebuild.playerjoingroup.spigot;

import fr.freebuild.playerjoingroup.core.Action;
import fr.freebuild.playerjoingroup.spigot.actions.ActionExecutionException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ActionExecutor {
    private final PlayerJoinGroup plugin;
    private final Object lock;
    private final Map<Integer, Action> commandIndex;

    public ActionExecutor(PlayerJoinGroup plugin) {
        this.plugin = plugin;
        this.lock = new Object();
        this.commandIndex = new HashMap<>();
    }

    private <T> void execute(int hashCode, T context) throws ActionExecutionException {
        synchronized (this.lock) {
            Action action = this.commandIndex.get(hashCode);

            if (action == null)
                throw new ActionExecutionException("Command with hashcode " + hashCode + " not found.", false);

            if (action.isExpired()) {
                this.commandIndex.remove(hashCode);
                throw new ActionExecutionException("Command with hashcode " + hashCode + " is expired.", true);
            }

            action.execute(context);
            this.commandIndex.remove(hashCode);
        }
    }

    public <T> void resolve(Action action, T context) {
        this.removeExpired();

        try {
            this.execute(action.hashCode(), context);
        } catch (ActionExecutionException err) {
            if (err.commandIsExpired())
                this.plugin.getLogger().warning("Command " + action.hashCode() + " is expired.");
            else
                this.add(action);
        }
    }

    public void add(Action action) {
        synchronized (this.lock) {
            this.commandIndex.put(action.hashCode(), action);
        }
    }
    public void removeExpired() {
        synchronized (this.lock) {
            Iterator<Map.Entry<Integer, Action>> iterator = this.commandIndex.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Action> entry = iterator.next();
                if (entry.getValue().isExpired())
                    iterator.remove();
            }
        }
    }
}
