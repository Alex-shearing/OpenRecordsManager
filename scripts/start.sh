#!/bin/bash

# mark to exit immediately if a pipeline returns a non-zero status
set -e

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Initialize an array for custom JVM args
JVM_ARGS=()

# Parse incoming arguments
while [[ $# -gt 0 ]]; do
    JVM_ARGS+=("$1")
    shift
done

# Find the server-core jar using glob matching
# (shopt -s nullglob ensures it handles zero matches gracefully)
shopt -s nullglob
JAR_FILES=("$SCRIPT_DIR"/server-core-*.jar)
shopt -u nullglob

if [ ${#JAR_FILES[@]} -eq 0 ]; then
    echo "Error: Application jar file not found" >&2
    exit 1
fi

# If multiple versions exist, pick the first one matching the glob pattern
JarPath="${JAR_FILES[0]}"

# Find Java Executable
JavaExe=""
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JavaExe="$JAVA_HOME/bin/java"
else
    JavaExe=$(command -v java 2>/dev/null)
fi

if [ -z "$JavaExe" ]; then
    echo "Error: Java executable could not be found. Please ensure Java is installed and JAVA_HOME or PATH is configured." >&2
    exit 1
fi

# Ensure the application runs from the distribution root so config.yml is loaded from the same directory.
cd "$SCRIPT_DIR"

# Execute the Java process.
# We expand custom JVM args first, followed by -jar and the target jar path.
exec "$JavaExe" "${JVM_ARGS[@]}" -jar "$JarPath"