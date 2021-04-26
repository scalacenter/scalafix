/*
rules = [
  "class:scalafix.test.NoDummy"
  "class:scalafix.test.NoNull"
]
*/
package test.escapeHatch

object AnchorAnnotationMixed {

  def aDummy0(x: Option[Any]): Unit = { // assert: NoDummy
    val bDummy = 0 // assert: NoDummy
    val cDummy = 0 // assert: NoDummy
    val foo = null // assert: NoNull
  }


  // Annotation mixed with 'scalafix:ok' anchor
  @SuppressWarnings(Array("NoDummy"))
  def aDummy1(x: Option[Any]): Unit = {
    val bDummy = 0
    val cDummy = 0 /* scalafix:ok NoDummy */ // assert: UnusedScalafixSuppression
    val foo = null // scalafix:ok NoNull
  }


  // Turning ON/OFF different rule using anchor within annotated method
  @SuppressWarnings(Array("NoDummy"))
  def aDummy2(x: Option[Any]): Unit = {
    val bDummy = 0
    // scalafix:off NoNull
    val cDummy = 0
    val foo1 = null
    // scalafix:on NoNull
    val foo2 = null // assert: NoNull
  }


  // Overlap - annotated method enclosed by anchors
  /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression
  @SuppressWarnings(Array("NoDummy"))
  def aDummy3(x: Option[Any]): Unit = {
    val bDummy = 0
    val cDummy = 0
  }
  // scalafix:on NoDummy
  val dDummy = 0 // assert: NoDummy



  // Overlap - anchors enclosed by annotated method
  @SuppressWarnings(Array("NoDummy"))
  def aDummy4(x: Option[Any]): Unit = {
    /* scalafix:off NoDummy */ // assert: UnusedScalafixSuppression
    val bDummy = 0
    // scalafix:on NoDummy
    val cDummy = 0 // NOTE: still OFF because it is within method
  }
  val eDummy = 0 // assert: NoDummy



  // Overlap - anchor ON before and OFF within annotated method
  // scalafix:off NoDummy
  def aDummy5(): Unit = ???

  @SuppressWarnings(Array("NoDummy"))
  def aDummy6(x: Option[Any]): Unit = {
    val bDummy = 0
    // scalafix:on NoDummy
    val cDummy = 0 // NOTE: still OFF because it is within method
  }
  val fDummy = 0 // assert: NoDummy



  // Overlap - anchor OFF within and ON after annotated method
  @SuppressWarnings(Array("NoDummy"))
  def aDummy7(x: Option[Any]): Unit = {
    val bDummy = 0
    // scalafix:off NoDummy
    val cDummy = 0
  }
  val gDummy = 0 // NOTE: still OFF because of OFF anchor above
  // scalafix:on NoDummy
  val hDummy = 0 // assert: NoDummy
}
