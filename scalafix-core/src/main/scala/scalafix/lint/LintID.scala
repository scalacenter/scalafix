package scalafix.lint

/** A unique identifier for this category of lint diagnostics
  *
  * The contract of id is that all diagnostics of the same "category" will have the same id.
  * For example, the DisableSyntax rule has a unique ID for each category such as "noSemicolon"
  * or "noTabs".
  *
  * @param rule the name of the rule that produced this diagnostic.
  * @param categoryID the sub-category within this rule, if any.
  *                   Empty if the rule only reports diagnostics of a single
  *                   category.
  */
final case class LintID(rule: String, categoryID: String) {
  def fullStringID: String =
    if (categoryID.isEmpty && rule.isEmpty) ""
    else if (categoryID.isEmpty) rule
    else if (rule.isEmpty) categoryID
    else s"$rule.$categoryID"
}

object LintID {
  val empty = LintID("", "")
}
