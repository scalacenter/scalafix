package scalafix.interfaces;

public enum ScalafixEvaluationError {
    /**
     * Something unexpected happened.
     */
    UnexpectedError,
    /**
     * A command-line argument parsed incorrectly.
     */
    CommandLineError,
    /**
     * No files were provided to Scalafix so nothing happened.
     */
    NoFilesError,
}
