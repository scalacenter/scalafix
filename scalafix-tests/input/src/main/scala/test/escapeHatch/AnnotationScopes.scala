/*
rules = [
  "class:scalafix.test.NoDummy"
]
*/
package test.escapeHatch

import scala.language.experimental.macros

object AnnotationScopes {

  // No suppressing
  type DummyType_0 = Any // assert: NoDummy

  trait DummyTrait_0 { // assert: NoDummy
    def aDummy: Unit // assert: NoDummy
    val bDummy: Int // assert: NoDummy
    var cDummy: Int // assert: NoDummy
    type Dummy // assert: NoDummy
  }

  object DummyObject_0 { // assert: NoDummy
    def aDummy = () // assert: NoDummy
  }

  class Foo_0[DummyTypeParam] // assert: NoDummy

  def dummyMacro_0: Unit = macro ??? // assert: NoDummy

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
  trait DummyTrait_1

  // Def/val/var/type declaration
  trait DummyTrait_2 { // assert: NoDummy
    @SuppressWarnings(Array("NoDummy"))
    def aDummy: Unit
    @SuppressWarnings(Array("NoDummy"))
    val bDummy: Int
    @SuppressWarnings(Array("NoDummy"))
    var cDummy: Int
    @SuppressWarnings(Array("NoDummy"))
    type Dummy
  }

  // Type parameter
  class Foo_1[@SuppressWarnings(Array("NoDummy")) DummyTypeParam]

  // Macro
  @SuppressWarnings(Array("NoDummy"))
  def dummyMacro_1: Unit = macro ???

  // Object
  @SuppressWarnings(Array("NoDummy"))
  object DummyObject_1 {
    def aDummy = ()
  }

  // Class
  @SuppressWarnings(Array("NoDummy"))
  class DummyClass_1(val aDummy: Int, val bDummy: Int) {
    val cDummy = 0
  }

  // Primary constructor
  class DummyClass_2 @SuppressWarnings(Array("NoDummy"))( // assert: NoDummy
                 val aDummy: Int, val bDummy: Int) {
    val cDummy = 0 // assert: NoDummy
  }

  // Primary constructor parameter
  class DummyClass_3( // assert: NoDummy
                 @SuppressWarnings(Array("NoDummy")) val aDummy: Int,
                 val bDummy: Int) { // assert: NoDummy
    val cDummy = 0 // assert: NoDummy
  }

  // Field
  class DummyClass_4 { // assert: NoDummy
    @SuppressWarnings(Array("NoDummy"))
    val cDummy = 0
    var dDummy = 0 // assert: NoDummy
  }

  // Secondary constructor
  class DummyClass_5 { // assert: NoDummy

    @SuppressWarnings(Array("NoDummy"))
    def this(eDummy: String, fDummy: String) {
      this
    }
  }

  // Secondary constructor parameter
  class DummyClass_6 { // assert: NoDummy

    def this(@SuppressWarnings(Array("NoDummy")) eDummy: String,
             fDummy: String) { // assert: NoDummy
      this
    }
  }

  // Method
  class DummyClass_7 { // assert: NoDummy

    @SuppressWarnings(Array("NoDummy"))
    def gDummy(hDummy: Int, iDummy: Int): Unit = {
      val jDummy = 0
      var hDummy = 0
    }
  }

  // Method parameter
  class DummyClass_8 { // assert: NoDummy

    def gDummy( // assert: NoDummy
                @SuppressWarnings(Array("NoDummy")) hDummy: Int,
                iDummy: Int): Unit = { // assert: NoDummy
      val jDummy = 0 // assert: NoDummy
      var hDummy = 0 // assert: NoDummy
    }
  }

  // Local variable
  class DummyClass_9 { // assert: NoDummy

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
