package vpvgui.exception;

/**
 * Created by phansen on 6/21/17.
 */
public class NoCuttingSiteFoundUpOrDownstreamException extends Exception {

    // Parameterless Constructor
    public NoCuttingSiteFoundUpOrDownstreamException() {}

    // Constructor that accepts a message
    public NoCuttingSiteFoundUpOrDownstreamException(String message)
    {
        super(message);
    }
}
