#!/usr/bin/env bash
set -eux

version=$1

scala212=2.12.20
scala213=2.13.17
scala3LTS=3.3.7
scala3Next=3.7.4

cs resolve \
  ch.epfl.scala:scalafix-interfaces:$version  \
  ch.epfl.scala:scalafix-core_2.12:$version  \
  ch.epfl.scala:scalafix-core_2.13:$version  \
  ch.epfl.scala:scalafix-reflect_$scala212:$version  \
  ch.epfl.scala:scalafix-reflect_$scala213:$version  \
  ch.epfl.scala:scalafix-rules_$scala212:$version  \
  ch.epfl.scala:scalafix-rules_$scala213:$version  \
  ch.epfl.scala:scalafix-rules_$scala3LTS:$version  \
  ch.epfl.scala:scalafix-rules_$scala3Next:$version  \
  ch.epfl.scala:scalafix-cli_$scala212:$version  \
  ch.epfl.scala:scalafix-cli_$scala213:$version  \
  ch.epfl.scala:scalafix-cli_$scala3LTS:$version  \
  ch.epfl.scala:scalafix-cli_$scala3Next:$version  \
  ch.epfl.scala:scalafix-testkit_$scala212:$version \
  ch.epfl.scala:scalafix-testkit_$scala213:$version \
  ch.epfl.scala:scalafix-testkit_$scala3LTS:$version \
  ch.epfl.scala:scalafix-testkit_$scala3Next:$version