package scalafix.interfaces;

public interface ScalafixRule {
    String name();
    String description();

    /**
     * @return Whether this rule is syntactic or semantic.
     */
    ScalafixRuleKind kind();

    /**
     * @return true if this rule is a linter meaning that it reports diagnostics
     * like error/warning/info messages without providing auto-fixes. Returns
     * false otherwise.
     *
     * Note, it's valid for a rule to be both a rewrite and linter.
     */
    boolean isLinter();

    /**
     * @return true if this rule is a rewrite meaning that it can automatically
     * fix problems in the source code. Returns false otherwise.
     *
     * Note, it's valid for a rule to be both a rewrite and linter.
     */
    boolean isRewrite();

}
