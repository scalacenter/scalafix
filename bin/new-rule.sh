#!/usr/bin/env bash
set -eux
rule=$1

ruleFile=scalafix-core/shared/src/main/scala/scalafix/internal/rule/${rule}.scala
input=scalafix-tests/input/src/main/scala/test/${rule}.scala
output=scalafix-tests/output/src/main/scala/test/${rule}.scala

# TODO(olafur) write boilerplate
touch $ruleFile
touch $input
touch $output
