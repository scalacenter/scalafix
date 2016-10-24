Contributing
===========

## Modules

- `core/` the rewrite rules
- `cli/` command line interface
- `sbtScalafmt/` SBT plugin

## Building

~~~ sh
$ sbt compile
~~~

## Testing

~~~ sh
$ sbt cli/test
$ sbt core/test
# For SBT plugin
$ sbt publishLocal # required for every change in core or cli
$ sbt scripted     # for changes in sbtScalafmt project
~~~

## TL;DR

If you are unsure about anything, don't hesitate to ask in the [gitter channel](https://gitter.im/scalacenter/scalafix).
