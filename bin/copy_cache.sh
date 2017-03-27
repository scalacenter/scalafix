#!/usr/bin/env bash
for dir in $(./bin/cache_directories.sh); do
  from=$1/$dir
  to=$2/$dir
  echo "Copying cache from $from to $to"
  test -d $from && \
    du -h -d 0 $from && \
    mkdir -p $to && \
    cp -a $from $to
done
