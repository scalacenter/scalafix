/*
rules = [
  "class:scalafix.test.EscapeHatchDummyLinter"
  "class:scalafix.test.EscapeHatchNoNulls"
]
*/
package test.escapeHatch

object AnnotationAnchorMixed {

  def aDummy0(x: Option[Any]): Unit = { // assert: EscapeHatchDummyLinter
    val bDummy = 0 // assert: EscapeHatchDummyLinter
    val cDummy = 0 // assert: EscapeHatchDummyLinter
    val foo = null // assert: EscapeHatchNoNulls
  }


//  // Annotation mixed with 'scalafix:ok' anchor
//  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
//  def aDummy1(x: Option[Any]): Unit = {
//    val bDummy = 0
//    val cDummy = 0 /* scalafix:ok EscapeHatchDummyLinter */ // assert: UnusedScalafixSupression.Disable
//    val foo = null // scalafix:ok EscapeHatchNoNulls
//  }
//
//
//  // Turning ON/OFF different rule using anchor within annotated method
//  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
//  def aDummy2(x: Option[Any]): Unit = {
//    val bDummy = 0
//    // scalafix:off EscapeHatchNoNulls
//    val cDummy = 0
//    val foo1 = null
//    // scalafix:on EscapeHatchNoNulls
//    val foo2 = null // assert: EscapeHatchNoNulls
//  }
//
//
//  // Overlap - annotated method enclosed by anchors
//  // scalafix:off EscapeHatchDummyLinter
//  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
//  def aDummy3(x: Option[Any]): Unit = {
//    val bDummy = 0
//    val cDummy = 0
//  }
//  // scalafix:on EscapeHatchDummyLinter
//  val dDummy = 0 // assert: EscapeHatchDummyLinter
//
//
//
//  // Overlap - anchors enclosed by annotated method
//  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
//  def aDummy4(x: Option[Any]): Unit = {
//    // scalafix:off EscapeHatchDummyLinter
//    val bDummy = 0
//    // scalafix:on EscapeHatchDummyLinter
//    val cDummy = 0 // NOTE: still OFF because it is within method
//  }
//  val eDummy = 0 // assert: EscapeHatchDummyLinter
//
//
//
//  // Overlap - anchor ON before and OFF within annotated method
//  // scalafix:off EscapeHatchDummyLinter
//  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
//  def aDummy5(x: Option[Any]): Unit = {
//    val bDummy = 0
//    // scalafix:on EscapeHatchDummyLinter
//    val cDummy = 0 // NOTE: still OFF because it is within method
//  }
//  val fDummy = 0 // assert: EscapeHatchDummyLinter
//
//
//
//  // Overlap - anchor OFF within and ON after annotated method
//  @SuppressWarnings(Array("EscapeHatchDummyLinter"))
//  def aDummy6(x: Option[Any]): Unit = {
//    val bDummy = 0
//    // scalafix:off EscapeHatchDummyLinter
//    val cDummy = 0
//  }
//  val gDummy = 0 // NOTE: still OFF because of OFF anchor above
//  // scalafix:on EscapeHatchDummyLinter
//  val hDummy = 0 // assert: EscapeHatchDummyLinter
}
