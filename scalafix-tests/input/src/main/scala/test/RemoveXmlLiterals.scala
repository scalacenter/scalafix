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
    val a = <div>{{</div>
    val b = <div>}}</div>
    // <div b="{{"/> >>> xml"""<div b="{{"/>"""
  }

  object J {
    <a>{1}{2}</a>
  }

  object K {
    null match { case <a></a> => }
  }

  object L {
    null match { case <a>{_*}</a> => }
  }

}
