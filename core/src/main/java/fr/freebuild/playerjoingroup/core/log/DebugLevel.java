package fr.freebuild.playerjoingroup.core.log;

import java.util.logging.Level;

public class DebugLevel extends Level {

        public static final Level DEBUG = new DebugLevel("DEBUG", Level.INFO.intValue() + 1);

        protected DebugLevel(String name, int value) {
            super(name, value);
        }
}
