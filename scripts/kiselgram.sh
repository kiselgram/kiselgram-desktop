#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/../Java/kiselgram-desktop-0.2.0.jar"

# Find Java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif command -v java &>/dev/null; then
    JAVA=java
else
    osascript -e 'display dialog "Kiselgram requires Java 21+.\n\nInstall from: https://adoptium.net" buttons {"OK"} default button 1 with icon stop'
    exit 1
fi

# Check Java version
JAVA_VER=$("$JAVA" -version 2>&1 | head -1 | sed 's/[^0-9.]//g' | cut -d. -f1)
if [ "$JAVA_VER" -lt 21 ]; then
    osascript -e 'display dialog "Kiselgram requires Java 21 or later.\nYour version: '"$JAVA_VER"'\n\nInstall from: https://adoptium.net" buttons {"OK"} default button 1 with icon stop'
    exit 1
fi

exec "$JAVA" --module-path "$DIR/../Java/javafx-lib" --add-modules javafx.controls,javafx.web -jar "$JAR"
