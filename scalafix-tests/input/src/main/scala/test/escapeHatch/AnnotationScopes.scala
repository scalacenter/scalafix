/*
rules = [
  "class:scalafix.test.NoDummy"
]
*/
package test.escapeHatch

object AnnotationScopes {

  // No suppressing
  type DummyType_0 = Any // assert: NoDummy

  trait DummyTrait_0 { // assert: NoDummy
    def aDummy = () // assert: NoDummy
  }

  object DummyObject_0 { // assert: NoDummy
    def aDummy = () // assert: NoDummy
  }

  class DummyClass_0( // assert: NoDummy
                 val aDummy: Int, // assert: NoDummy
                 val bDummy: Int) { // assert: NoDummy

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    def this(eDummy: String, // assert: NoDummy
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    def gDummy( // assert: NoDummy
              hDummy: Int, // assert: NoDummy
              iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Type
  @SuppressWarnings(Array("NoDummy"))
  type DummyType_1 = Any

  // Trait
  @SuppressWarnings(Array("NoDummy"))
  trait DummyTrait_1 {
    def aDummy = ()
  }

  // Object
  @SuppressWarnings(Array("NoDummy"))
  object DummyObject_1 {
    def aDummy = ()
  }

  // Class
  @SuppressWarnings(Array("NoDummy"))
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
  class DummyClass_2 @SuppressWarnings(Array("NoDummy"))( // assert: NoDummy
                 val aDummy: Int, val bDummy: Int) {

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    def this(eDummy: String, // assert: NoDummy
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    def gDummy( // assert: NoDummy
                hDummy: Int, // assert: NoDummy
                iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Primary constructor parameter
  class DummyClass_3( // assert: NoDummy
                 @SuppressWarnings(Array("NoDummy")) val aDummy: Int,
                 val bDummy: Int) { // assert: NoDummy

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    def this(eDummy: String, // assert: NoDummy
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    def gDummy( // assert: NoDummy
                hDummy: Int, // assert: NoDummy
                iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Field
  class DummyClass_4( // assert: NoDummy
                 val aDummy: Int, // assert: NoDummy
                 val bDummy: Int) { // assert: NoDummy

    @SuppressWarnings(Array("NoDummy"))
    val cDummy = 0
    @SuppressWarnings(Array("NoDummy"))
    var dDummy = 0

    def this(eDummy: String, // assert: NoDummy
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    def gDummy( // assert: NoDummy
                hDummy: Int, // assert: NoDummy
                iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Secondary constructor
  class DummyClass_5( // assert: NoDummy
                 val aDummy: Int, // assert: NoDummy
                 val bDummy: Int) { // assert: NoDummy

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    @SuppressWarnings(Array("NoDummy"))
    def this(eDummy: String, fDummy: String) {
      this(eDummy.toInt, fDummy.toInt)
    }

    def gDummy( // assert: NoDummy
                hDummy: Int, // assert: NoDummy
                iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Secondary constructor parameter
  class DummyClass_6( // assert: NoDummy
                 val aDummy: Int, // assert: NoDummy
                 val bDummy: Int) { // assert: NoDummy

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    def this(@SuppressWarnings(Array("NoDummy")) eDummy: String,
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    def gDummy( // assert: NoDummy
                hDummy: Int, // assert: NoDummy
                iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Method
  class DummyClass_7( // assert: NoDummy
                 val aDummy: Int, // assert: NoDummy
                 val bDummy: Int) { // assert: NoDummy

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    def this(eDummy: String, // assert: NoDummy
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    @SuppressWarnings(Array("NoDummy"))
    def gDummy(hDummy: Int, iDummy: Int): Unit = {
      val jDummy = 0
      var hDummy = 0
    }
  }

  // Method parameter
  class DummyClass_8( // assert: NoDummy
                 val aDummy: Int, // assert: NoDummy
                 val bDummy: Int) { // assert: NoDummy

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    def this(eDummy: String, // assert: NoDummy
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    def gDummy( // assert: NoDummy
                @SuppressWarnings(Array("NoDummy")) hDummy: Int,
                iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Local variable
  class DummyClass_9( // assert: NoDummy
                 val aDummy: Int, // assert: NoDummy
                 val bDummy: Int) { // assert: NoDummy

    val cDummy = 0 // assert: NoDummy
    var dDummy = 0 // assert: NoDummy

    def this(eDummy: String, // assert: NoDummy
             fDummy: String) { // assert: NoDummy
      this(eDummy.toInt, fDummy.toInt) // assert: NoDummy
    }

    def gDummy( // assert: NoDummy
                hDummy: Int, // assert: NoDummy
                iDummy: Int): Unit = { // assert: NoDummy
      @SuppressWarnings(Array("NoDummy"))
      val jDummy = 0
      @SuppressWarnings(Array("NoDummy"))
      var hDummy = 0
    }
  }

  // nested scopes
  def outter = {
    @SuppressWarnings(Array("NoDummy"))
    def aDummy1 = {
      def aDummy11 = {
        def aDummy111 = ()
        def aDummy112 = ()
        def aDummy113 = ()
      }
    }

    def aDummy2 = { // assert: NoDummy
      @SuppressWarnings(Array("NoDummy"))
      def aDummy21 = {
        def aDummy211 = ()
        def aDummy212 = ()
        def aDummy213 = ()
      }
    }

    def aDummy3 = { // assert: NoDummy
      def aDummy31 = { // assert: NoDummy
        @SuppressWarnings(Array("NoDummy"))
        def aDummy311 = ()
        def aDummy312 = () // assert: NoDummy
        def aDummy313 = () // assert: NoDummy
      }
    }
  }
}
