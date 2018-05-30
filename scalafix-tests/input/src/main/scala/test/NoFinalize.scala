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

  class InferedUnit1 {
    override protected def finalize() = () // assert: NoFinalize
  }

  class InferedUnit {
    override def finalize() = () // assert: NoFinalize
  }

  class Example {
    override protected def finalize() = () /* assert: NoFinalize
                           ^
    finalize should not be used
    */
  }

  class Negative {
    def ok = 1
    def finalize(in: Int): Int = in
  }
}
