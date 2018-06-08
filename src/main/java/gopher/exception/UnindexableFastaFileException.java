package gopher.exception;

public class UnindexableFastaFileException extends GopherException {
    public UnindexableFastaFileException() {}

    public UnindexableFastaFileException(String message)
    {
        super(message);
    }
}
