#!/bin/sh
# Enable the Funput keyboard extension on a simulator for UI tests
# (VietnameseTypingUITests). Adds the extension to the enabled-keyboards list
# and disconnects the Mac hardware keyboard so the software keyboard appears.
#
# Usage: Scripts/uitest-enable-keyboard.sh [udid]   (default: the booted sim)
#
# Run after the app has been installed at least once (the extension must be on
# the device before iOS will honour the AppleKeyboards entry). Restart the
# Simulator app afterwards if it was already running so the hardware-keyboard
# preference is picked up.
set -eu

UDID="${1:-booted}"

if [ "$UDID" = "booted" ]; then
    UDID="$(xcrun simctl list devices booted | grep -oE '[0-9A-F-]{36}' | head -1)"
    if [ -z "$UDID" ]; then
        echo "uitest-enable-keyboard: no booted simulator found" >&2
        exit 1
    fi
fi

# TextInput reads the enabled-keyboards list from the GLOBAL defaults domain,
# not from the Settings app's com.apple.Preferences domain.
xcrun simctl spawn "$UDID" defaults write -g AppleKeyboards -array \
    "en_US@sw=QWERTY;hw=Automatic" \
    "app.funput.funput.Keyboard"
xcrun simctl spawn "$UDID" defaults write -g AppleKeyboardsExpanded -int 1

# Software keyboard must be visible for the test to tap keys.
defaults write com.apple.iphonesimulator DevicePreferences -dict-add "$UDID" \
    '{ ConnectHardwareKeyboard = 0; }'

echo "uitest-enable-keyboard: Funput keyboard enabled on $UDID"
