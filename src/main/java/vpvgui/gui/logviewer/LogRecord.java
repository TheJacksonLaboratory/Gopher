package vpvgui.gui.logviewer;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class LogRecord {
    private String timestamp;
    private Level  level;
    private String context;
    private String message;
    private static DateFormat df = new SimpleDateFormat("dd-mm-yyyy kk:mm:ss", Locale.US);

    public LogRecord(Level level, String date, String context, String message) {
        this.timestamp = date;
        this.level     = level;
        this.context   = context;
        this.message   = message;
    }

   public String getTimestamp() {
        return timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getContext() {
        return context;
    }

    public String getMessage() {
        return message;
    }
}

