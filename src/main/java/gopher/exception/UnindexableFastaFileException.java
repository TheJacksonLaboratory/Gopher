package gopher.exception;

public class UnindexableFastaFileException extends VPVException {
    public UnindexableFastaFileException() {}

    public UnindexableFastaFileException(String message)
    {
        super(message);
    }
}
