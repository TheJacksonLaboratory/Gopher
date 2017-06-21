package vpvgui.exception;

/**
 * Created by phansen on 6/20/17.
 */
public class IntegerOutOfRangeException extends Exception {

    // Parameterless Constructor
    public IntegerOutOfRangeException() {}

    // Constructor that accepts a message
    public IntegerOutOfRangeException(String message)
    {
        super(message);
    }
}
