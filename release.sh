#!/usr/bin/env bash

set -eo pipefail

function call_sed(){
PATTERN="$1"
FILENAME="$2"

# Mac needs a space between sed's inplace flag and extension
if [ "$(uname)" == "Darwin" ]; then
    sed -E -i '' "$PATTERN" "$FILENAME"
else
    sed -E -i "$PATTERN" "$FILENAME"
fi
}

FILE_BUILD_GRADLE="algoliasearch/common.gradle"
FILE_API_CLIENT="algoliasearch/src/main/java/com/algolia/search/saas/Client.java"

if [ $# -ne 1 ]; then
    echo "$0 | A script to release new versions automatically"
    echo "Usage: $0 VERSION_CODE"
    exit -1
fi

VERSION_CODE=$1

echo "Updating version code to $VERSION_CODE and committing..."
call_sed "s/(PUBLISH_VERSION =) '.*'/\1 '$VERSION_CODE'/" $FILE_BUILD_GRADLE
call_sed "s/(private final static String version =) \".*\"/\1 \"$VERSION_CODE\"/" $FILE_API_CLIENT

git add $FILE_BUILD_GRADLE $FILE_API_CLIENT && git commit -m "Release $VERSION_CODE"
git --no-pager show --name-status --format="short"

echo "Updating artifacts..."
./gradlew uploadArchives 1>/dev/null

echo "Success! Closing and releasing new version..."
./gradlew closeAndPromoteRepository 1>/dev/null
