package scalafix.interfaces;

/**
 * An error occurred while loading an instance of {@link Scalafix}.
 */
public class ScalafixException extends Exception {
    static final long serialVersionUID = 118L;
    public ScalafixException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScalafixException(String message) {
        super(message);
    }
}
