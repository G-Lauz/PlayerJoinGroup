package fr.freebuild.playerjoingroup.core.action;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class ActionExecutor {
    private final Object lock;
    private final Logger logger;
    private final Map<Integer, Action> commandIndex;

    public ActionExecutor(Logger logger) {
        this.logger = logger;
        this.lock = new Object();
        this.commandIndex = new HashMap<>();
    }

    private <T> void execute(int hashCode, T context) throws ActionExecutionException {
        Action action = null;
        synchronized (this.lock) {
            action = this.commandIndex.remove(hashCode);

            if (action == null)
                throw new ActionExecutionException("Command with hashcode " + hashCode + " not found.", false);

            if (action.isExpired())
                throw new ActionExecutionException("Command with hashcode " + hashCode + " is expired.", true);
        }

        if (action != null)
            action.execute(context);
    }

    public <T> void resolve(Action action, T context) {
        this.removeExpired();

        try {
            this.execute(action.hashCode(), context);
        } catch (ActionExecutionException err) {
            if (err.commandIsExpired())
                this.logger.warning("Command " + action.hashCode() + " is expired.");
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
