#!/usr/bin/env bash
set -eux

version=$1

coursier fetch \
  ch.epfl.scala:scalafix-interfaces:$version  \
  ch.epfl.scala:scalafix-core_2.11:$version  \
  ch.epfl.scala:scalafix-core_2.12:$version  \
  ch.epfl.scala:scalafix-reflect_2.11.12:$version  \
  ch.epfl.scala:scalafix-reflect_2.12.8:$version  \
  ch.epfl.scala:scalafix-cli_2.11.12:$version  \
  ch.epfl.scala:scalafix-cli_2.12.8:$version  \
  ch.epfl.scala:scalafix-testkit_2.11.12:$version  \
  ch.epfl.scala:scalafix-testkit_2.12.8:$version \
  -r sonatype:releases
