/*
rules = DisableSyntax

DisableSyntax.noFinalize = true
*/
package test

case object NoFinalize {

  class Simple {
    override def finalize(): Unit = () // assert: DisableSyntax.noFinalize
  }

  class Protected {
    override protected def finalize(): Unit = () // assert: DisableSyntax.noFinalize
  }

  class InferedUnit1 {
    override protected def finalize() = () // assert: DisableSyntax.noFinalize
  }

  class InferedUnit {
    override def finalize() = () // assert: DisableSyntax.noFinalize
  }

  class Example {
    override protected def finalize() = () /* assert: DisableSyntax.noFinalize
                           ^
    finalize should not be used
    */
  }

  class Negative {
    def ok = 1
    def finalize(in: Int): Int = in
  }
}
