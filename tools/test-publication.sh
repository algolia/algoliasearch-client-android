#!/usr/bin/env bash
# ============================================================================ #
# TEST PUBLICATION
# ============================================================================ #
# SUMMARY
# -------
# Test that the demo project can link against the (locally) published version.
# Meant to be run before actually promoting the release.
# ============================================================================ #

# Reflection.
SELF_ROOT=`cd \`dirname "$0"\` && pwd`
SELF_NAME=`basename "$0"`

PROJECT_ROOT=`cd "$SELF_ROOT/.." && pwd`

usage()
{
    echo "Usage:"
    echo "   $SELF_NAME <module_name> <version_no>"
}


if [[ $# -lt 2 ]]; then
    usage 1>&2
    exit 1
fi

MODULE_NAME="$1"
VERSION_NO="$2"

# Retrieve the latest demo sources.
DEMO_DIR=`mktemp -d`
DEMO_REPO="algolia/algolia-android-demo.git"
git clone "git@github.com:$DEMO_REPO" "$DEMO_DIR"
cd "$DEMO_DIR"

# Patch the version.
echo "Targeting $MODULE_NAME $VERSION_NO"
sed -E -i '' "s/compile 'com\.algolia:algoliasearch-android:[0-9.]+@aar'/compile 'com.algolia:${MODULE_NAME}:${VERSION_NO}@aar'/g" "app/build.gradle"

# Use the local repo.
MVNREP_DIR="$PROJECT_ROOT/algoliasearch/build/mvnrep"
echo "Targeting repository '$MVNREP'"
cat >> build.gradle <<EOF
allprojects {
    repositories {
        maven {
            url "file://$MVNREP_DIR"
        }
    }
}
EOF

# Build with the new version.
echo "Building..."
./gradlew assembleRelease
rc=$?

# Clean up.
rm -rf "$DEMO_DIR"

exit $rc
