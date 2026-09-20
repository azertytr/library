#!/bin/bash
# Installs (or reuses) an Android SDK sufficient to build this project with
# ./gradlew, and points Gradle at it via local.properties. Safe to re-run:
# it no-ops when the required components are already present.
set -euo pipefail

SDK_ROOT="${ANDROID_HOME:-/opt/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"
PLATFORM="android-35"
BUILD_TOOLS="35.0.0"

REPO_ROOT="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"

have_required_components() {
  [ -x "$SDK_ROOT/platform-tools/adb" ] &&
    [ -d "$SDK_ROOT/platforms/$PLATFORM" ] &&
    [ -d "$SDK_ROOT/build-tools/$BUILD_TOOLS" ]
}

if have_required_components; then
  echo "Android SDK already present at $SDK_ROOT - skipping install."
else
  echo "Installing Android SDK to $SDK_ROOT..."
  mkdir -p "$SDK_ROOT/cmdline-tools"

  if [ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    tmp_zip="$(mktemp -d)/cmdline-tools.zip"
    curl -fsSL -o "$tmp_zip" \
      "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    unzip -q "$tmp_zip" -d "$SDK_ROOT/cmdline-tools"
    rm -rf "$SDK_ROOT/cmdline-tools/latest"
    mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
    rm -f "$tmp_zip"
  fi

  sdkmanager="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  yes | "$sdkmanager" --sdk_root="$SDK_ROOT" --licenses >/dev/null 2>&1 || true
  "$sdkmanager" --sdk_root="$SDK_ROOT" \
    "platform-tools" "platforms;$PLATFORM" "build-tools;$BUILD_TOOLS" >/dev/null
fi

echo "sdk.dir=$SDK_ROOT" >"$REPO_ROOT/local.properties"

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export ANDROID_HOME=$SDK_ROOT"
    echo "export ANDROID_SDK_ROOT=$SDK_ROOT"
  } >>"$CLAUDE_ENV_FILE"
fi

echo "Android SDK ready at $SDK_ROOT (local.properties written)."
