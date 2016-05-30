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

# Check that the working repository is clean (without any changes, neither staged nor unstaged).
if [[ ! -z `git status --porcelain` ]]; then
    echo "ERROR: Working copy not clean! Aborting." 1>&2
    echo "Please revert or commit any pending changes before releasing." 1>&2
    exit 1
fi 

$SELF_ROOT/tools/update-version.sh $VERSION_CODE

for flavor in online offline
do
    echo "========== Processing flavor '$flavor' =========="
    $SELF_ROOT/select-flavor.sh $flavor
    $SELF_ROOT/gradlew clean
    $SELF_ROOT/gradlew uploadArchives
    if [[ $flavor = "online" ]]; then
        module_name="algoliasearch-android"
    elif [[ $flavor = "offline" ]]; then
        module_name="algoliasearch-offline-android"
    fi
    # Dump the contents that has been published, just for the sake of manual checking.
    $SELF_ROOT/tools/dump-local-mvnrep.sh $module_name
    echo "---------- Testing publication ----------"
    $SELF_ROOT/tools/test-publication.sh $module_name $VERSION_CODE
done

echo "SUCCESS: closing and releasing new version..."
$SELF_ROOT/gradlew closeAndPromoteRepository 1>/dev/null

# Revert flavor to original.
git checkout $SELF_ROOT/algoliasearch/build.gradle

# Commit to Git.
git add .
git commit -m "Version $VERSION_CODE"
git tag $VERSION_CODE
git push --tags origin master
