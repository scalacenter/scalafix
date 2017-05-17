/*
rewrites = RemoveXmlLiterals
 */
package test

class RemoveXmlLiterals {
  object A {
    <div></div>
  }

  object B {
    val a = <div b="Hello"/>
    val b = <a>\</a>
  }

  object C {
    val bar = "bar"
    <div>{bar}</div>
  }

  object D {
    val foo =
      <div>
        <span>Hello</span>
      </div>
  }

  object E {
    val foo =
      <div>
        <span>{"Hello"}</span>
      </div>
  }

  object F {
    <foo bar={"Hello"}/>
  }

  object G {
    <a>{<a>{"Hello"}</a>}</a>
  }

  object H {
    <div>$</div>
  }

  object I {
    <div>{{</div>
  }

//  <<< SKIP protect curly brace 2
//  object J {
//      <div b="{{"/>
//  }
//  >>>
//  import scala.xml.quote._
//  object J {
//    xml"""<div b="{{"/>"""
//  }

  object K {
    <a>{1}{2}</a>
  }

  object L {
    null match { case <a></a> => }
  }

  object M {
    null match { case <a>{_*}</a> => }
  }

}
