#!/usr/bin/env bash

set -eo pipefail

SELF_ROOT=$(cd $(dirname "$0") && pwd)

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

FILE_BUILD_GRADLE="$SELF_ROOT/algoliasearch/common.gradle"
FILE_API_CLIENT="$SELF_ROOT/algoliasearch/src/main/java/com/algolia/search/saas/Client.java"

if [ $# -ne 1 ]; then
    echo "$0 | A script to release new versions automatically"
    echo "Usage: $0 VERSION_CODE"
    exit -1
fi

VERSION_CODE=$1

echo "Updating version code to $VERSION_CODE..."
call_sed "s/(PUBLISH_VERSION =) '.*'/\1 '$VERSION_CODE'/" $FILE_BUILD_GRADLE
call_sed "s/(private final static String version =) \".*\"/\1 \"$VERSION_CODE\"/" $FILE_API_CLIENT

git diff $FILE_BUILD_GRADLE $FILE_API_CLIENT

echo "Uploading flavor 'online'..."
$SELF_ROOT/select-flavor.sh online
$SELF_ROOT/gradlew uploadArchives 1>/dev/null

echo "Uploading flavor 'offline'"
$SELF_ROOT/select-flavor.sh online
$SELF_ROOT/gradlew uploadArchives 1>/dev/null

echo "Success! Closing and releasing new version..."
$SELF_ROOT/gradlew closeAndPromoteRepository 1>/dev/null

# Revert flavor to original.
git checkout $SELF_ROOT/algoliasearch/build.gradle

echo "IMPORANT: Remember to git commit & tag & push"
