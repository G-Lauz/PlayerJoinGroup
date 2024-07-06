package fr.freebuild.playerjoingroup.core.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;

public class DebugFormatter extends Formatter {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public String format(java.util.logging.LogRecord record) {
        if (record.getLevel() == DebugLevel.DEBUG)
            return this.formatMessage(record);

        return super.formatMessage(record);
    }

    public String formatMessage(java.util.logging.LogRecord record) {
        StringBuilder builder = new StringBuilder();

        // Header
        builder.append(ANSI_WHITE).append(this.getDate(record.getMillis())).append(" ");
        builder.append("[").append(ANSI_PURPLE).append("DEBUG").append(ANSI_WHITE).append("] ");
        builder.append("[").append(Thread.currentThread().getName()).append("/").append(record.getSourceMethodName()).append("]: ");

        builder.append(ANSI_RESET).append(record.getMessage());
        return builder.toString();
    }

    public String getDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(timestamp);
        return dateFormat.format(date);
    }
}
