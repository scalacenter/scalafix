package scalafix.interfaces;

/**
 * A source position pointing to a range location in code.
 */
public interface ScalafixPosition {

    /**
     * Pretty-print a message at this source location.
     *
     * @param severity the severity of this message.
     * @param message the actual message contents.
     * @return a formatted string containing the message, severity and caret
     * pointing to the source code location.
     */
    String formatMessage(String severity, String message);

    /**
     * @return The character offset at the start of this range location.
     */
    int startOffset();

    /**
     * @return The 0-based line number of the start of this range location.
     */
    int startLine();

    /**
     * @return The 0-based column number of the start of this range location.
     */
    int startColumn();

    /**
     * @return The character offset at the end of this range location.
     */
    int endOffset();

    /**
     * @return The 0-based line number of the start of this range location.
     */
    int endLine();

    /**
     * @return The 0-based column number of the end of this range location.
     */
    int endColumn();

    /**
     * @return The source input of this range position.
     */
    ScalafixInput input();

}
