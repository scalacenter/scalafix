package scalafix.interfaces;

/**
 * An error occurred while classloading an instance of {@link Scalafix}.
 */
public class ScalafixException extends Exception {
    static final long serialVersionUID = 118L;
    public ScalafixException(String message, Throwable cause) {
        super(message, cause);
    }
}
