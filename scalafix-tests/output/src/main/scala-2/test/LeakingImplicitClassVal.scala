package test

object LeakingImplicitClassVal {
  implicit class XtensionVal(private val int: Int) extends AnyVal {
    def doubled: Int = int + int
  }

  implicit class XtensionFinalVal(private final val int: Int) extends AnyVal {
    def doubled: Int = int + int
  }

  implicit class XtensionAnnotatedVal(@transient private val str: String) extends AnyVal {
    def doubled: String = str + str
  }

  implicit class XtensionAnnotatedFinalVal(@transient private final val str: String) extends AnyVal {
    def doubled: String = str + str
  }

  implicit class XtensionAnnotatedProtectedVal(@transient protected val int: Int) extends AnyVal {
    def doubled: Int = int + int
  }
}
