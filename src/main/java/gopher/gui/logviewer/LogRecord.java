package gopher.gui.logviewer;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

class LogRecord {
    private String timestamp;
    private Level  level;
    private String context;
    private String message;
    private static DateFormat df = new SimpleDateFormat("dd-MM-yyyy kk:mm:ss", Locale.US);

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

