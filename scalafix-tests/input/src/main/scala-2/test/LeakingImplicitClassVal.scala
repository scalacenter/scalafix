/*
rules = LeakingImplicitClassVal
 */
package test

object LeakingImplicitClassVal {
  implicit class XtensionVal(val int: Int) extends AnyVal {
    def doubled: Int = int + int
  }

  implicit class XtensionFinalVal(final val int: Int) extends AnyVal {
    def doubled: Int = int + int
  }

  implicit class XtensionAnnotatedVal(@transient val str: String) extends AnyVal {
    def doubled: String = str + str
  }

  implicit class XtensionAnnotatedFinalVal(@transient final val str: String) extends AnyVal {
    def doubled: String = str + str
  }

  implicit class XtensionAnnotatedProtectedVal(@transient protected val int: Int) extends AnyVal {
    def doubled: Int = int + int
  }
}
