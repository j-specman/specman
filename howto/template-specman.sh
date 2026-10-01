#!/bin/sh
# Generated from howto/template-specman.sh by 'mvn package' - do not edit target/specman.sh directly

# Check strategy before starting Specman:
#   1. Is 'java' on the PATH at all? If not, abort with a download hint.
#   2. Is its version new enough (>= MIN_JAVA_VERSION)? If not, abort with a download hint.
#   3. Locate the Specman jar: prefer one next to this script (portable/zip layout, no setup needed),
#      otherwise fall back to $SPECMAN_HOME/<jar> (fileserver install layout).
#      If neither exists, abort and tell the user both locations that were checked.
#   4. All checks passed: start Specman.

JAR_NAME="specman-@VERSION@-jar-with-dependencies.jar"
MIN_JAVA_VERSION=@MIN_JAVA_VERSION@
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if ! command -v java >/dev/null 2>&1; then
    echo "Java not found on PATH." >&2
    echo "Specman requires Java $MIN_JAVA_VERSION or higher. Download: https://adoptium.net" >&2
    exit 1
fi

JAVA_VERSION_FULL=$(java --version 2>&1 | awk 'NR==1{print $2}')
JAVA_MAJOR=$(echo "$JAVA_VERSION_FULL" | cut -d. -f1)

if [ "$JAVA_MAJOR" -lt "$MIN_JAVA_VERSION" ] 2>/dev/null; then
    echo "Found Java $JAVA_VERSION_FULL, but Specman requires Java $MIN_JAVA_VERSION or higher." >&2
    echo "Download: https://adoptium.net" >&2
    exit 1
fi

if [ -f "$SCRIPT_DIR/$JAR_NAME" ]; then
    JAR_PATH="$SCRIPT_DIR/$JAR_NAME"
elif [ -n "$SPECMAN_HOME" ] && [ -f "$SPECMAN_HOME/$JAR_NAME" ]; then
    JAR_PATH="$SPECMAN_HOME/$JAR_NAME"
else
    echo "Could not find $JAR_NAME." >&2
    echo "Looked in:" >&2
    echo "  $SCRIPT_DIR" >&2
    if [ -n "$SPECMAN_HOME" ]; then
        echo "  $SPECMAN_HOME" >&2
    else
        echo "  (SPECMAN_HOME is not set)" >&2
    fi
    echo "Either place the jar next to this script, or set SPECMAN_HOME to the directory containing it." >&2
    exit 1
fi

exec java -jar "$JAR_PATH" "$@"
