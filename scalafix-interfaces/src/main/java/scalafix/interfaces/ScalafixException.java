package scalafix.interfaces;

/**
 * An error occurred while classloading an instance of {@link Scalafix}.
 */
public class ScalafixException extends Exception {
    ScalafixException(String message, Exception cause) {
        super(message, cause);
    }
}
