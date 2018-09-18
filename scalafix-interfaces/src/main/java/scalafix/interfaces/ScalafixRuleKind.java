package scalafix.interfaces;

public enum ScalafixRuleKind {

    /**
     * This rule requires input sources to be compiled with the Scala compiler and
     * the SemanticDB compiler plugin enabled. For SemanticDB installation instruction,
     * consult the
     * <a href="https://scalameta.org/docs/semanticdb/guide.html">
     * SemanticDB guide
     * </a>.
     */
    SEMANTIC,

    /**
     * This rule can run on input sources without compilation. The only required input
     * for syntactic rules is a filename and text contents.
     */
    SYNTACTIC;

    /**
     * @return true if this rule is semantic, false otherwise.
     */
    public boolean isSemantic() {
        return this == SEMANTIC;
    }

    /**
     * @return true if this rule is syntactic, false otherwise.
     */
    public boolean isSyntactic() {
        return this == SYNTACTIC;
    }
}
