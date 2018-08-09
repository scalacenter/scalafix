package scalafix.interfaces;

/**
 * A unique identifier for this category of lint diagnostics
 * <p>
 * The contract of id is that all diagnostics of the same "category" will have the same id.
 * For example, the DisableSyntax rule has a unique ID for each category such as "noSemicolon"
 * or "noTabs".
 */
public interface ScalafixLintID {

    /**
     * @return The name of the rule that produced this diagnostic. For example, "Disable".
     */
    String ruleName();

    /**
     * @return the sub-category within this rule, if any.
     * Empty if the rule only reports diagnostics of a single
     * category. For example, "get" when the full lint ID
     * is "Disable.get".
     */
    String categoryID();

}
