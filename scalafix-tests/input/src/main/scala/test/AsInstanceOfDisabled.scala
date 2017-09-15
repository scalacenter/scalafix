/*
rule = AsInstanceOfDisabled
*/
package test

case object AsInstanceOfDisabled {

  case class B()
  val y = B().asInstanceOf[String]// assert: AsInstanceOfDisabled
  val z = 1.asInstanceOf[String]// assert: AsInstanceOfDisabled
  val x = "2".asInstanceOf[Int]// assert: AsInstanceOfDisabled
  val w = List(1, 2, 3).asInstanceOf[Seq[String]]// assert: AsInstanceOfDisabled
}
