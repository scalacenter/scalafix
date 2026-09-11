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
    CHECK,

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

    /**
     * Use when the client triggers the run as a side effect of something else, as opposed to an explicit, interactive invocation. Write fixed contents in-place, with a custom configuration if it exists.
     */
    IN_PLACE_TRIGGERED,

    /**
     * Report linter errors only, without printing a diff of the fixed contents.
     *
     * Unlike {@link #CHECK}, files that could be fixed do not cause an error to be reported.
     *
     * Does not write to files.
     */
    DRY_RUN
}
