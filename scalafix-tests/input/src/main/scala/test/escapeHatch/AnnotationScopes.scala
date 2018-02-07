/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
]
*/
package test.escapeHatch

object AnnotationScopes {

  // No suppressing
  type DummyType_0 = Any // assert: EscapeHatchDummyLinter

  trait DummyTrait_0 { // assert: EscapeHatchDummyLinter
    def aDummy = () // assert: EscapeHatchDummyLinter
  }

  object DummyObject_0 { // assert: EscapeHatchDummyLinter
    def aDummy = () // assert: EscapeHatchDummyLinter
  }

  class DummyClass_0( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, // assert: EscapeHatchDummyLinter
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    def this(eDummy: String, // assert: EscapeHatchDummyLinter
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    def gDummy( // assert: EscapeHatchDummyLinter
              hDummy: Int, // assert: EscapeHatchDummyLinter
              iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      val jDummy = 0 // assert: EscapeHatchDummyLinter
      var hDummy = 0 // assert: EscapeHatchDummyLinter
    }
  }

  // Type
  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
  type DummyType_1 = Any

  // Trait
  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
  trait DummyTrait_1 {
    def aDummy = ()
  }

  // Object
  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
  object DummyObject_1 {
    def aDummy = ()
  }

  // Class
  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
  class DummyClass_1(val aDummy: Int, val bDummy: Int) {

    val cDummy = 0
    var dDummy = 0

    def this(eDummy: String, fDummy: String) {
      this(eDummy.toInt, fDummy.toInt)
    }

    def gDummy(hDummy: Int, iDummy: Int): Unit = {
      val jDummy = 0
      var hDummy = 0
    }
  }

  // Primary constructor
  class DummyClass_2 @SuppressWarnings(Array("EscapeHatchDummyLinter"))( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, val bDummy: Int) {

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    def this(eDummy: String, // assert: EscapeHatchDummyLinter
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    def gDummy( // assert: EscapeHatchDummyLinter
                hDummy: Int, // assert: EscapeHatchDummyLinter
                iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      val jDummy = 0 // assert: EscapeHatchDummyLinter
      var hDummy = 0 // assert: EscapeHatchDummyLinter
    }
  }

  // Primary constructor parameter
  class DummyClass_3( // assert: EscapeHatchDummyLinter
                 @SuppressWarnings(Array("EscapeHatchDummyLinter")) val aDummy: Int,
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    def this(eDummy: String, // assert: EscapeHatchDummyLinter
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    def gDummy( // assert: EscapeHatchDummyLinter
                hDummy: Int, // assert: EscapeHatchDummyLinter
                iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      val jDummy = 0 // assert: EscapeHatchDummyLinter
      var hDummy = 0 // assert: EscapeHatchDummyLinter
    }
  }

  // Field
  class DummyClass_4( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, // assert: EscapeHatchDummyLinter
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    @SuppressWarnings(Array("EscapeHatchDummyLinter"))
    val cDummy = 0
    @SuppressWarnings(Array("EscapeHatchDummyLinter"))
    var dDummy = 0

    def this(eDummy: String, // assert: EscapeHatchDummyLinter
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    def gDummy( // assert: EscapeHatchDummyLinter
                hDummy: Int, // assert: EscapeHatchDummyLinter
                iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      val jDummy = 0 // assert: EscapeHatchDummyLinter
      var hDummy = 0 // assert: EscapeHatchDummyLinter
    }
  }

  // Secondary constructor
  class DummyClass_5( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, // assert: EscapeHatchDummyLinter
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    @SuppressWarnings(Array("EscapeHatchDummyLinter"))
    def this(eDummy: String, fDummy: String) {
      this(eDummy.toInt, fDummy.toInt)
    }

    def gDummy( // assert: EscapeHatchDummyLinter
                hDummy: Int, // assert: EscapeHatchDummyLinter
                iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      val jDummy = 0 // assert: EscapeHatchDummyLinter
      var hDummy = 0 // assert: EscapeHatchDummyLinter
    }
  }

  // Secondary constructor parameter
  class DummyClass_6( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, // assert: EscapeHatchDummyLinter
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    def this(@SuppressWarnings(Array("EscapeHatchDummyLinter")) eDummy: String,
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    def gDummy( // assert: EscapeHatchDummyLinter
                hDummy: Int, // assert: EscapeHatchDummyLinter
                iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      val jDummy = 0 // assert: EscapeHatchDummyLinter
      var hDummy = 0 // assert: EscapeHatchDummyLinter
    }
  }

  // Method
  class DummyClass_7( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, // assert: EscapeHatchDummyLinter
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    def this(eDummy: String, // assert: EscapeHatchDummyLinter
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    @SuppressWarnings(Array("EscapeHatchDummyLinter"))
    def gDummy(hDummy: Int, iDummy: Int): Unit = {
      val jDummy = 0
      var hDummy = 0
    }
  }

  // Method parameter
  class DummyClass_8( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, // assert: EscapeHatchDummyLinter
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    def this(eDummy: String, // assert: EscapeHatchDummyLinter
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    def gDummy( // assert: EscapeHatchDummyLinter
                @SuppressWarnings(Array("EscapeHatchDummyLinter")) hDummy: Int,
                iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      val jDummy = 0 // assert: EscapeHatchDummyLinter
      var hDummy = 0 // assert: EscapeHatchDummyLinter
    }
  }

  // Local variable
  class DummyClass_9( // assert: EscapeHatchDummyLinter
                 val aDummy: Int, // assert: EscapeHatchDummyLinter
                 val bDummy: Int) { // assert: EscapeHatchDummyLinter

    val cDummy = 0 // assert: EscapeHatchDummyLinter
    var dDummy = 0 // assert: EscapeHatchDummyLinter

    def this(eDummy: String, // assert: EscapeHatchDummyLinter
             fDummy: String) { // assert: EscapeHatchDummyLinter
      this(eDummy.toInt, fDummy.toInt) // assert: EscapeHatchDummyLinter
    }

    def gDummy( // assert: EscapeHatchDummyLinter
                hDummy: Int, // assert: EscapeHatchDummyLinter
                iDummy: Int): Unit = { // assert: EscapeHatchDummyLinter
      @SuppressWarnings(Array("EscapeHatchDummyLinter"))
      val jDummy = 0
      @SuppressWarnings(Array("EscapeHatchDummyLinter"))
      var hDummy = 0
    }
  }

  // nested scopes
  def outter = {
    @SuppressWarnings(Array("EscapeHatchDummyLinter"))
    def aDummy1 = {
      def aDummy11 = {
        def aDummy111 = ()
        def aDummy112 = ()
        def aDummy113 = ()
      }
    }

    def aDummy2 = { // assert: EscapeHatchDummyLinter
      @SuppressWarnings(Array("EscapeHatchDummyLinter"))
      def aDummy21 = {
        def aDummy211 = ()
        def aDummy212 = ()
        def aDummy213 = ()
      }
    }

    def aDummy3 = { // assert: EscapeHatchDummyLinter
      def aDummy31 = { // assert: EscapeHatchDummyLinter
        @SuppressWarnings(Array("EscapeHatchDummyLinter"))
        def aDummy311 = ()
        def aDummy312 = () // assert: EscapeHatchDummyLinter
        def aDummy313 = () // assert: EscapeHatchDummyLinter
      }
    }
  }
}
