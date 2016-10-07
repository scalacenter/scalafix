#!/usr/bin/env bash
set -e

echo "Running nailgun integration test..."
ng ng-stop || true

cwd=$(pwd)
cd cli/target/pack
make install
cd $cwd

echoOriginalContents() {
  cat << EOF
object app {
  def main() {
    println(1)
  }
}
EOF
}

echoExpectedContents() {
  cat << EOF
object app {
  def main(): Unit = {
    println(1)
  }
}
EOF
}
original=target/original.scala
expected=target/expected.scala

mkdir -p target
echoOriginalContents > ${original}
echoExpectedContents > ${expected}
./bin/scalafix_ng -i ${original}

# Assert there is no diff
diff ${original} ${expected}

echo "Done!"
