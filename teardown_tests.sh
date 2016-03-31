#!/bin/bash

set -e
set -o pipefail

if [ -z "$FILE" ]; 
then 
    FILE="algoliasearch/src/test/java/com/algolia/search/saas/Helpers.java"
fi

echo "Restoring Helper file..."
mv $FILE.bak $FILE
rm $FILE.tmp
