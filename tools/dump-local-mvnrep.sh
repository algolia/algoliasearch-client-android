#!/usr/bin/env bash
# ============================================================================ #
# DUMP LOCAL MVNREP                                                            #
# ============================================================================ #
# SUMMARY
# -------
# Print a summary of the local Maven repository content. This script is
# typically used to check that the last publication's contents are OK.
# ============================================================================ #

# Reflection.
SELF_ROOT=$(cd $(dirname "$0") && pwd)
PROJECT_ROOT=`cd "$SELF_ROOT/.." && pwd`

MVNREP_DIR="$PROJECT_ROOT/algoliasearch/build/mvnrep"
COM_ALGOLIA_DIR="$MVNREP_DIR/com/algolia"

if [ $# -lt 1 ]; then
    echo "Please specify module name" 1>&2
    exit
fi

if [ "$(uname)" == "Darwin" ]; then
    XARGS_REPLACE=-J
else
    XARGS_REPLACE=-I
fi

# Dump the module with the specified name.
dumpModule()
{
    module_name="$1"

    echo "================================================================================"
    echo "Module: $module_name"
    echo "================================================================================"
    echo "Code"
    echo "--------------------------------------------------------------------------------"
    ls "$COM_ALGOLIA_DIR/$module_name/$VERSION/$module_name"-*.aar | sort | tail -n 1 | xargs $XARGS_REPLACE % \
    unzip -p % classes.jar > "$TMP_DIR/tmp.zip"; unzip -l "$TMP_DIR/tmp.zip"

    echo "--------------------------------------------------------------------------------"
    echo "Javadoc"
    echo "--------------------------------------------------------------------------------"
    ls "$COM_ALGOLIA_DIR/$module_name/$VERSION/$module_name"-*-javadoc.jar | sort | tail -n 1 | xargs $XARGS_REPLACE % \
    unzip -l %

    echo "--------------------------------------------------------------------------------"
    echo "POM"
    echo "--------------------------------------------------------------------------------"
    ls "$COM_ALGOLIA_DIR/$module_name/$VERSION/$module_name"-*.pom | sort | tail -n 1 | xargs $XARGS_REPLACE % \
    cat %
}

# Create temporary directory.
TMP_DIR=`mktemp -d`

# Retrieve version number from root Gradle script.
VERSION=`cat "$PROJECT_ROOT/algoliasearch/common.gradle" | grep -E "PUBLISH_VERSION\s*=\s*'[0-9.]+(-SNAPSHOT)?'" | cut -d "'" -f 2`
echo "Version: $VERSION"

dumpModule "$1"

# Clean-up.
rm -rf "$TMP_DIR"
