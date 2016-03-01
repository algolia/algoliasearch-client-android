#!/bin/bash

set -e
set -o pipefail

echo "Restoring Helper file..."
mv $FILE.bak $FILE
rm $FILE.tmp
