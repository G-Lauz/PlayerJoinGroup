package fr.freebuild.playerjoingroup.core.log;

import java.util.logging.Logger;

public class DebugLogger {
    private final Logger logger;

    private boolean isActive;

    public DebugLogger(Logger logger, boolean isActive) {
        this.logger = logger;
        this.isActive = isActive;
    }

    public void debug(String message) {
        if (this.isActive)
            this.logger.info("[DEBUG] " + message);
    }

    public void info(String message) {
        this.logger.info(message);
    }

    public void warning(String message) {
        this.logger.warning(message);
    }

    public void severe(String message) {
        this.logger.severe(message);
    }
}
