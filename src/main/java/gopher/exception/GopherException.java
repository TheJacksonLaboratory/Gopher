package gopher.exception;

public class GopherException extends Exception {

    public GopherException() {}

    public GopherException(String message) {
        super(message);
    }

    public GopherException(String message, Throwable throwable) {
        super(message, throwable);
    }
}