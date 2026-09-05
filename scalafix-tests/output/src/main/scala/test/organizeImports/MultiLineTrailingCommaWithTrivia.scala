package test.organizeImports

// The trailing comma is separated from the last importee by trivia (whitespace here,
// but comments behave the same); it must still be detected and preserved after reordering.
import scala.collection.immutable.{
  BitSet,
  HashMap,
  TreeMap,
}

object MultiLineTrailingCommaWithTrivia
