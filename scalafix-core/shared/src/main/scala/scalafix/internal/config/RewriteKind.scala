package scalafix.internal.config

// Boolean enum, either Syntactic or Semantic.
sealed trait RewriteKind {
  def isSyntactic: Boolean = this == RewriteKind.Syntactic
}

object RewriteKind {
  def apply(syntactic: Boolean): RewriteKind =
    if (syntactic) Syntactic else Semantic
  case object Syntactic extends RewriteKind
  case object Semantic extends RewriteKind
}
