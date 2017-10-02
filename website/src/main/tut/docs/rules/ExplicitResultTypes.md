---
layout: docs
title: ExplicitResultTypes
---
## ExplicitResultTypes

### ⚠️ Experimental
Dotty requires implicit `val` and `def` to explicitly annotate result types.
This rewrite inserts the inferred type from the compiler for implicit definitions that are missing an explicit result type.

For example,

```scala
// before
implicit val tt = liftedType

// after
implicit val tt: TypedType[R] = liftedType
```

### Configuration

```scala
ExplicitResultTypes.memberKind = [Val, Def, Var]
ExplicitResultTypes.memberVisibility = [Public, Protected]
// Experimental, shorten fully qualified names and insert missing imports
// By default, names are fully qualified and prefixed with _root_.
unsafeShortenNames = true // false by default.
ExplicitResultTypes.unsafeShortenNames = true // false by default.
```

### Known limitations

- [#324](https://github.com/{{ site.githubOwner }}/{{ site.githubRepo }}/issues/324)

- This rewrite inserts fully qualified names by default, even if the short name is available in scope. Blocked by [scalameta/scalameta#1075](https://github.com/scalameta/scalameta/issues/1075).

- Inserts type annotations only for implicit definitions by default. To configure the rule to run on non-implicit definitions

- The rewrite may insert conflicting names in scope. This is especially true with `unsafeShortenNames=true`, but may happen by default for path dependent types.
