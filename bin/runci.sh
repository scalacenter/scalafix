#!/bin/sh
set -eux
TEST=${1}

case "$TEST" in
  "scalafmt" )
    ./scalafmt --test
    ;;
  * )
    sbt \
      "-no-colors" \
      "^^ $CI_SBT_VERSION" \
      $TEST
    ;;
esac

