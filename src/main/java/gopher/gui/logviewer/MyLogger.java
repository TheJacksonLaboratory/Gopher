package gopher.gui.logviewer;

public record MyLogger(Log log, String context) {

    public void log(LogRecord record) {
        log.offer(record);
    }

    public void debug(String msg,String date) {
        log(new LogRecord(Level.DEBUG, date,context, msg));
    }

    public void trace(String msg,String date) {
        log(new LogRecord(Level.TRACE,date, context, msg));
    }


    public void info(String msg,String date) {
        log(new LogRecord(Level.INFO,date, context, msg));
    }

    public void warn(String msg,String date) {
        log(new LogRecord(Level.WARN,date, context, msg));
    }

    public void error(String msg,String date) {
        log(new LogRecord(Level.ERROR, date,context, msg));
    }

    public Log getLog() {
        return log;
    }
}
