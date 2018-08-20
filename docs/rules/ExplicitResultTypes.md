---
layout: docs
id: ExplicitResultTypes
title: ExplicitResultTypes
---

**⚠️ Experimental**

This rewrite inserts the inferred type from the compiler for implicit
definitions that are missing an explicit result type.

For example,

```scala
// before
implicit val tt = liftedType

// after
implicit val tt: TypedType[R] = liftedType
```

But for local implicit `val` and `def` it's not needed and will not be added by
default.

```scala
def foo = {
  // this is OK
  implicit val localImplicit = 2
}
```

### Configuration

This rule has several different options.

```scala mdoc:passthrough
import scalafix.internal.config._
```

```scala mdoc:passthrough
println(
scalafix.website.rule("ExplicitResultTypes", ExplicitResultTypesConfig.default)
)
```

### Known limitations

- [#324](https://github.com/{{ site.githubOwner
  }}/{{ site.githubRepo }}/issues/324)

- This rewrite inserts fully qualified names by default, even if the short name
  is available in scope. Blocked by
  [scalameta/scalameta#1075](https://github.com/scalameta/scalameta/issues/1075).

- Inserts type annotations only for implicit definitions by default. To
  configure the rule to run on non-implicit definitions

- The rewrite may insert conflicting names in scope. This is especially true
  with `unsafeShortenNames=true`, but may happen by default for path dependent
  types.
