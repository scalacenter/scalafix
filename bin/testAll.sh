#!/bin/bash
set -e

sbt clean coverage test
sbt "; publishLocal ; scripted ; cli/pack"
sbt coverageAggregate

