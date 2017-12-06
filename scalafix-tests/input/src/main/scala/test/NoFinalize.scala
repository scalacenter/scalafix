/*
rules = NoFinalize
*/
package test

case object NoFinalize {

  class Simple {
    override def finalize(): Unit = () // assert: NoFinalize
  }

  class Protected {
    override protected def finalize(): Unit = () // assert: NoFinalize
  }

  class InferedUnit {
    override protected def finalize() = () // assert: NoFinalize
  }

  class Example {
    override protected def finalize() = () /* assert: NoFinalize
                           ^
    finalizer may be never called and have a severe performance penalty
    */
  }

  class Negative {
    def ok = 1
    def finalize(in: Int): Int = in
  }
}