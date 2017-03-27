#!/usr/bin/env bash
set -e
du -h -d 1 $HOME/.ivy2/cache
du -h -d 2 $HOME/.sbt
find $HOME/.sbt -name "*.lock" -type f -delete
find $HOME/.ivy2/cache -name "ivydata-*.properties" -type f -delete
