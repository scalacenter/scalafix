package scalafix.internal.config

// Boolean enum, either Syntactic or Semantic.
sealed trait RuleKind {
  def isSyntactic: Boolean = this == RuleKind.Syntactic
}

object RuleKind {
  def apply(syntactic: Boolean): RuleKind =
    if (syntactic) Syntactic else Semantic
  case object Syntactic extends RuleKind
  case object Semantic extends RuleKind
}
