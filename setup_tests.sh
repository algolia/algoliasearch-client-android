#!/bin/bash

set -e
set -o pipefail

if [ -z "$FILE" ];
then
    FILE="algoliasearch/src/test/java/com/algolia/search/saas/Helpers.java"
fi
echo "Helper file: $FILE."
cp $FILE $FILE.bak

echo "Replacing environment variable..."
sed -i.tmp "s/%ALGOLIA_APPLICATION_ID%/${ALGOLIA_APPLICATION_ID}/g" $FILE
sed -i.tmp "s/%ALGOLIA_API_KEY%/${ALGOLIA_API_KEY}/g" $FILE
sed -i.tmp "s/%PLACES_APPLICATION_ID%/${PLACES_APPLICATION_ID}/g" $FILE
sed -i.tmp "s/%PLACES_API_KEY%/${PLACES_API_KEY}/g" $FILE
sed -i.tmp "s/%JOB_NUMBER%/${TRAVIS_JOB_NUMBER}/g" $FILE
