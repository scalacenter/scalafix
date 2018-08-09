package scalafix.interfaces;

/**
 * The mode for running the command-line interface.
 */
public enum ScalafixMainMode {

    /**
     * Default value, write fixed contents in-place.
     */
    IN_PLACE,

    /**
     * Report error if fixed contents does not match original file contents.
     *
     * Does not write to files.
     */
    TEST,

    /**
     * Print fixed output to stdout.
     *
     * Does not write to files.
     */
    STDOUT,

    /**
     * Instead of reporting linter error messages, write suppression comments in-place.
     */
    AUTO_SUPPRESS_LINTER_ERRORS,

}
