package scalafix.interfaces;

/**
 * A code representing a category of errors that happen while running Scalafix.
 */
public enum ScalafixError {
    /**
     * Something unexpected happened.
     */
    UnexpectedError,
    /**
     * A source file failed to parse.
     */
    ParseError,
    /**
     * A command-line argument parsed incorrectly.
     */
    CommandLineError,
    /**
     * A semantic rewrite was run on a source file that has no associated <code>META-INF/semanticdb/.../*.semanticdb</code>.
     *
     * Typical causes of this error include
     *
     * <ul>
     *     <li>Incorrect --classpath, make sure the classpath is compiled with the SemanticDB compiler plugin</li>
     *     <li>Incorrect --sourceroot, if the classpath is compiled with a custom <code>-P:semanticdb:sourceroot:{path}</code>
     *     then make sure the provided --sourceroot is correct.
     *     </li>
     * </ul>
     */
    MissingSemanticdbError,
    /**
     * The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin.
     *
     * To resolve this error re-compile the project and re-run Scalafix.
     */
    StaleSemanticdbError,
    /**
     * When run with {@link ScalafixMainMode#TEST}, this error is returned when a file on disk does not match
     * the file contents if it was fixed with Scalafix.
     */
    TestError,
    /**
     * A linter error was reported.
     */
    LinterError,
    /**
     * No files were provided to Scalafix so nothing happened.
     */
    NoFilesError
}
