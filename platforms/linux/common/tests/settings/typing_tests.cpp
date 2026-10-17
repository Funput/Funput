// `Settings::sameTyping`: which settings change how a word is composed, and which do
// not. It decides whether a reload may interrupt a half-typed word, so a field on the
// wrong side of the line either mangles a word or leaves a stale setting behind.

#include <doctest/doctest.h>

#include "settings/settings.h"

using namespace funput;

TEST_CASE("identical settings compose the same way") {
    const Settings settings;
    CHECK(settings.sameTyping(Settings{}));
}

TEST_CASE("each field that shapes a word is a difference") {
    const Settings base;
    Settings other;

    other = base;
    other.method = Method::Telex;
    CHECK_FALSE(base.sameTyping(other));

    other = base;
    other.toneStyle = ToneStyle::Traditional;
    CHECK_FALSE(base.sameTyping(other));

    other = base;
    other.enabled = !base.enabled;
    CHECK_FALSE(base.sameTyping(other));

    other = base;
    other.smartRestore = !base.smartRestore;
    CHECK_FALSE(base.sameTyping(other));

    other = base;
    other.eagerRestore = !base.eagerRestore;
    CHECK_FALSE(base.sameTyping(other));

    other = base;
    other.spellCheck = !base.spellCheck;
    CHECK_FALSE(base.sameTyping(other));

    other = base;
    other.autoCapitalize = !base.autoCapitalize;
    CHECK_FALSE(base.sameTyping(other));

    other = base;
    other.extraOnsets = ExtraOnsetLetters::all();
    CHECK_FALSE(base.sameTyping(other));
}

TEST_CASE("the gõ tắt table, its switches and the hotkeys are not a difference") {
    const Settings base;
    Settings other = base;

    other.shortcuts = {{"vn", "Việt Nam"}};
    other.shortcutsEnabled = !base.shortcutsEnabled;
    other.shortcutSmartCase = !base.shortcutSmartCase;
    other.shortcutsInEnglish = !base.shortcutsInEnglish;
    other.toggleHotkey = Hotkey::AltShift;
    other.flipHotkey = FlipHotkey::CtrlShiftZ;

    CHECK(base.sameTyping(other));
}
