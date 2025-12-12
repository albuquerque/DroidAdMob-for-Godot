#!/bin/bash

# Quick Build Script for DroidAdMob for Godot Plugin
# This script helps build the plugin with the correct Java version

echo "=== DroidAdMob for Godot Plugin Build Script ==="
echo ""

# Check if Java 17 is available (required for Android)
if command -v /usr/libexec/java_home &> /dev/null; then
    # macOS
    echo "Detecting Java installations on macOS..."

    if JAVA_17=$(/usr/libexec/java_home -v 17 2>/dev/null); then
        echo "✓ Found Java 17 at: $JAVA_17"
        export JAVA_HOME="$JAVA_17"
    elif JAVA_21=$(/usr/libexec/java_home -v 21 2>/dev/null); then
        echo "⚠ Found Java 21 at: $JAVA_21"
        echo "⚠ WARNING: Java 21 may have compatibility issues with Android Gradle Plugin"
        echo "⚠ Recommend installing Java 17 instead"
        export JAVA_HOME="$JAVA_21"
    else
        echo "✗ Java 17 or 21 not found!"
        echo ""
        echo "Please install Java 17:"
        echo "  brew install openjdk@17"
        echo ""
        echo "Or download from: https://adoptium.net/"
        exit 1
    fi
else
    # Linux/Windows
    echo "Please ensure JAVA_HOME is set to Java 17"
    if [ -z "$JAVA_HOME" ]; then
        echo "✗ JAVA_HOME is not set!"
        echo ""
        echo "Set JAVA_HOME to your Java 17 installation:"
        echo "  export JAVA_HOME=/path/to/jdk-17"
        exit 1
    fi
fi

echo ""
echo "Using Java at: $JAVA_HOME"
echo "Java version:"
"$JAVA_HOME/bin/java" -version
echo ""

# Build the plugin
echo "Building DroidAdMob for Godot plugin..."
echo ""

./gradlew clean assemble

if [ $? -eq 0 ]; then
    echo ""
    echo "=== ✓ Build Successful! ==="
    echo ""
    echo "Plugin location:"
    echo "  plugin/demo/addons/DroidAdMob/"
    echo ""
    echo "Contains:"
    echo "  - bin/debug/DroidAdMob-debug.aar"
    echo "  - bin/release/DroidAdMob-release.aar"
    echo "  - plugin.cfg"
    echo "  - export_plugin.gd"
    echo "  - admob.gd"
    echo ""
else
    echo ""
    echo "=== ✗ Build Failed ==="
    echo ""
    echo "Please check the error messages above."
    echo "Common issues:"
    echo "  - Java version incompatibility (use Java 17 or 21)"
    echo "  - Missing Android SDK"
    echo "  - Network issues (Gradle needs to download dependencies)"
    exit 1
fi
