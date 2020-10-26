package scalafix.interfaces;

public enum FileEvaluationError {
    /**
     * Something unexpected happened.
     */
    UnexpectedError,
    /**
     * A source file failed to parse.
     */
    ParseError,
    /**
     * The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin.
     *
     * To resolve this error re-compile the project and re-run Scalafix.
     */
    StaleSemanticdbError,
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
}
