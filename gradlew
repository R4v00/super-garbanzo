#!/usr/bin/env sh

set -e

DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

GRADLEW_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
JAVA_OPTS=${JAVA_OPTS:-""}
GRADLE_OPTS=${GRADLE_OPTS:-""}

if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

exec "$JAVA_CMD" $JAVA_OPTS -jar "$GRADLEW_JAR" "$@"
