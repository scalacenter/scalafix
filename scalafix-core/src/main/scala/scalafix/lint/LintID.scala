package scalafix.lint

/** A unique identifier for this category of lint diagnostics
  *
  * The contract of id is that all diagnostics of the same "category" will have the same id.
  * For example, the DisableSyntax rule has a unique ID for each category such as "noSemicolon"
  * or "noTabs".
  *
  * @param rule the name of the rule that produced this diagnostic.
  * @param category the sub-category within this rule, if any.
  *                 Empty if the rule only reports diagnostics of a single
  *                 category.
  */
final case class LintID(rule: String, category: String) {
  def fullID: String =
    if (category.isEmpty && rule.isEmpty) ""
    else if (category.isEmpty) rule
    else if (rule.isEmpty) category
    else s"$rule.$category"
}

object LintID {
  val empty = LintID("", "")
}
