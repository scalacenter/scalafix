/*
rules = "ExplicitResultTypes"
ExplicitResultTypes.skipSimpleDefinitions = ["Lit"]
*/
package test.explicitResultTypes

object ImplicitMembersWithSkipSimpleDefinition {

    val x = 42

    implicit var y = 43

    val map_x = null.asInstanceOf[Map[Int,String]]

    implicit val map_y = null.asInstanceOf[Map[Int,String]]

}
