/*
rules = DisableSyntax

DisableSyntax.noFinalize = true
*/
package test.disableSyntax

case object NoFinalize {

  class Simple {
    override def finalize(): Unit = () // assert: DisableSyntax.finalize
  }

  class Protected {
    override protected def finalize(): Unit = () // assert: DisableSyntax.finalize
  }

  class InferedUnit1 {
    override protected def finalize() = () // assert: DisableSyntax.finalize
  }

  class InferedUnit {
    override def finalize() = () // assert: DisableSyntax.finalize
  }

  class Example {
    override protected def finalize() = () /* assert: DisableSyntax.finalize
                           ^^^^^^^^
    finalize should not be used
    */
  }

  class Negative {
    def ok = 1

    def finalize(in: Int): Int = in
  }
}