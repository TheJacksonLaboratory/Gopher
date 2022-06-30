package gopher.gui.logviewer;


class LogRecord {
    private final String timestamp;
    private final Level  level;
    private final String context;
    private final String message;

     LogRecord(Level level, String date, String context, String message) {
        this.timestamp = date;
        this.level     = level;
        this.context   = context;
        this.message   = message;
    }

    String getTimestamp() {
        return timestamp;
    }

     Level getLevel() {
        return level;
    }

     String getContext() {
        return context;
    }

    public String getMessage() {
        return message;
    }
}

