#!/bin/bash
set -euo pipefail

export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOWNLOAD_DIR="$PROJECT_DIR/desktop-site/download"
APP_NAME="Kiselgram"
JAR="$SCRIPT_DIR/build/libs/kiselgram-desktop-1.0.0.jar"
BUNDLE_DIR="$DOWNLOAD_DIR/$APP_NAME.app"
ZIP_ARM="$DOWNLOAD_DIR/Kiselgram-mac-arm64.zip"

GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
JAVAFX_VERSION="21"

# Find JavaFX jars
JAVAFX_JARS=$(find "$GRADLE_CACHE" -path "*/org.openjfx/javafx-*" -name "*-mac-aarch64.jar" | sort)

echo "=== Building jar ==="
cd "$SCRIPT_DIR"
./gradlew jar

echo ""
echo "=== Building Apple Silicon .app ==="
rm -rf "$BUNDLE_DIR"
mkdir -p "$BUNDLE_DIR/Contents/MacOS"
mkdir -p "$BUNDLE_DIR/Contents/Java/javafx-lib"
mkdir -p "$BUNDLE_DIR/Contents/Resources"

# Copy jar
cp "$JAR" "$BUNDLE_DIR/Contents/Java/"

# Copy JavaFX native jars
for jfx in $JAVAFX_JARS; do
    cp "$jfx" "$BUNDLE_DIR/Contents/Java/javafx-lib/"
done

# Copy Info.plist
cp "$SCRIPT_DIR/scripts/Info.plist" "$BUNDLE_DIR/Contents/"

# Create launcher script
cat > "$BUNDLE_DIR/Contents/MacOS/Kiselgram" << 'LAUNCHER'
#!/bin/bash
DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$DIR/Java/kiselgram-desktop-1.0.0.jar"
JAVAFX="$DIR/Java/javafx-lib"

exec java \
    --module-path "$JAVAFX" \
    --add-modules javafx.controls,javafx.web \
    -jar "$JAR"
LAUNCHER

chmod +x "$BUNDLE_DIR/Contents/MacOS/Kiselgram"

echo "  -> Built $BUNDLE_DIR"

echo ""
echo "=== Zipping Apple Silicon .app ==="
rm -f "$ZIP_ARM"
cd "$DOWNLOAD_DIR"
zip -r -q "$ZIP_ARM" "$APP_NAME.app"
echo "  -> $ZIP_ARM ($(du -sh "$ZIP_ARM" | cut -f1))"

echo ""
echo "=== Building Intel zip ==="
INTEL_DIR="$DOWNLOAD_DIR/Kiselgram-mac-intel"
rm -rf "$INTEL_DIR"
mkdir -p "$INTEL_DIR/Java/javafx-lib"

cp "$JAR" "$INTEL_DIR/Java/"

cp "$SCRIPT_DIR/scripts/kiselgram.sh" "$INTEL_DIR/Kiselgram.sh"
chmod +x "$INTEL_DIR/Kiselgram.sh"

cat > "$INTEL_DIR/README.txt" << 'EOF'
Kiselgram Desktop - Intel Mac

Requirements:
  - macOS (Intel)
  - Java 21+ (Download from https://adoptium.net)

To run:
  1. Open Terminal in this folder
  2. Run: ./Kiselgram.sh

Or double-click Kiselgram.sh (may need to right-click -> Open first time).
EOF

ZIP_INTEL="$DOWNLOAD_DIR/Kiselgram-mac-intel.zip"
rm -f "$ZIP_INTEL"
cd "$DOWNLOAD_DIR"
zip -r -q "$ZIP_INTEL" "$(basename "$INTEL_DIR")"
rm -rf "$INTEL_DIR"
echo "  -> $ZIP_INTEL ($(du -sh "$ZIP_INTEL" | cut -f1))"

echo ""
echo "=== Building Windows zip ==="
WIN_DIR="$DOWNLOAD_DIR/Kiselgram-windows"
rm -rf "$WIN_DIR"
mkdir -p "$WIN_DIR/Java/javafx-lib"

cp "$JAR" "$WIN_DIR/Java/"
cp "$SCRIPT_DIR/scripts/kiselgram.bat" "$WIN_DIR/Kiselgram.bat"

cat > "$WIN_DIR/README.txt" << 'EOF'
Kiselgram Desktop - Windows

Requirements:
  - Windows 10/11
  - Java 21+ (Download from https://adoptium.net)

To run:
  Double-click Kiselgram.bat
EOF

ZIP_WIN="$DOWNLOAD_DIR/Kiselgram-windows.zip"
rm -f "$ZIP_WIN"
cd "$DOWNLOAD_DIR"
zip -r -q "$ZIP_WIN" "$(basename "$WIN_DIR")"
rm -rf "$WIN_DIR"
echo "  -> $ZIP_WIN ($(du -sh "$ZIP_WIN" | cut -f1))"

echo ""
echo "=== Done ==="
