#!/usr/bin/env bash
set -eux
rewrite=$1

touch scalafix-core/src/main/scala/scalafix/rewrite/${rewrite}.scala
touch scalafix-tests/input/src/main/scala/test/${rewrite}.scala
touch scalafix-tests/output/src/main/scala/test/${rewrite}.scala
