#!/bin/bash
set -e

sbt -Dsbt.home.ivy=/drone/cache/ivy2 clean coverage test
sbt -Dsbt.home.ivy=/drone/cache/ivy2 "; publishLocal ; scripted ; cli/pack"
sbt -Dsbt.home.ivy=/drone/cache/ivy2 coverageAggregate
