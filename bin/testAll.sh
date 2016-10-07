#!/bin/bash
set -e


sbt -Dpersist.enable clean semanticCompile/compile
#sbt coverage test # coverage seems to break the semantic tests :/
sbt test
sbt "; publishLocal ; scripted ; cli/pack"
#sbt coverageAggregate

# Integration tests
./bin/nailgun_integration_test.sh

