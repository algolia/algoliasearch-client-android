#!/usr/bin/env ksh
# ============================================================================ #
# SELECT FLAVOR                                                                #
# ============================================================================ #
# SUMMARY
# -------
# This script is used to select the flavor (online or offline) that will be
# compiled. For an explanation why this is necessary, see `doc/build.md`.
# ============================================================================ #

# Reflection.
SELF_ROOT=`cd \`dirname "$0"\`; pwd`
SELF_NAME=`basename "$0"`

# Print usage information to stdout.
usage()
{
    echo "Usage:"
    echo "    $SELF_NAME <online|offline>"
}

# Check command-line arguments.
if [ $# -lt 1 ]; then
    echo "Please specify a flavor!" 1>&2
    usage 1>&2
    exit 1
fi

FLAVOR="$1"
if [[ $FLAVOR != "online" && $FLAVOR != "offline" ]]; then
    echo "Unknown flavor '$FLAVOR'" 1>&2
    usage 1>&2
    exit 1
fi

# Do it!
ln -sfh "build-$FLAVOR.gradle" "$SELF_ROOT/algoliasearch/build.gradle"
