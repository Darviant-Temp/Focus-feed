#!/bin/bash
set -e
for f in *__*; do
  [ -f "$f" ] || continue
  d="${f//__//}"
  mkdir -p "$(dirname "$d")"
  mv "$f" "$d"
done
find app -type f | sort
