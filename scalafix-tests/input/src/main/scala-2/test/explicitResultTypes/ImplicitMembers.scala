/*
rules = "ExplicitResultTypes"
ExplicitResultTypes.memberKind = [Val]
ExplicitResultTypes.memberVisibility = [Public]
*/
package test.explicitResultTypes

object ImplicitMembers {

    implicit val ai = Set(List("ai"))

    implicit var bi = Set(List("bi"))

    val x = Set(List("x"))

    var y = Set(List("y"))

    private implicit val pri_ai = Set(List("pri_ai"))

    private implicit var pri_bi = Set(List("pri_bi"))

    private val pri_x = Set(List("pri_x"))

    private var pri_y = Set(List("pri_y"))
}