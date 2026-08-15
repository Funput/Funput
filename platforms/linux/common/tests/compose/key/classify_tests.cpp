// Key classification. This is where the two shells were easiest to drift apart:
// each spelled the hotkey presets out separately, against its own key type.

#include <doctest/doctest.h>

#include "compose/key/classify.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("toggle presets need Ctrl and their own key") {
    CHECK(matchesToggle(ctrl('`'), Hotkey::CtrlBacktick));
    CHECK_FALSE(matchesToggle(ascii('`'), Hotkey::CtrlBacktick)); // no Ctrl
    CHECK_FALSE(matchesToggle(ctrl(' '), Hotkey::CtrlBacktick));  // wrong key

    CHECK(matchesToggle(ctrl(' '), Hotkey::CtrlSpace));
    CHECK_FALSE(matchesToggle(ctrl('`'), Hotkey::CtrlSpace));

    // Alt+Shift is a chord that needs release tracking; neither shell implements
    // it, so it must never match a key press.
    CHECK_FALSE(matchesToggle(ctrl('`'), Hotkey::AltShift));
    CHECK_FALSE(matchesToggle(ctrl(' '), Hotkey::AltShift));
}

TEST_CASE("flip presets need Ctrl+Shift and match either letter case") {
    CHECK(matchesFlip(ctrlShift('z'), FlipHotkey::CtrlShiftZ));
    CHECK(matchesFlip(ctrlShift('Z'), FlipHotkey::CtrlShiftZ));
    CHECK(matchesFlip(ctrlShift('x'), FlipHotkey::CtrlShiftX));
    CHECK(matchesFlip(ctrlShift('X'), FlipHotkey::CtrlShiftX));

    CHECK_FALSE(matchesFlip(ctrl('z'), FlipHotkey::CtrlShiftZ)); // no Shift
    CHECK_FALSE(matchesFlip(ctrlShift('z'), FlipHotkey::CtrlShiftX));
    CHECK_FALSE(matchesFlip(ctrlShift('z'), FlipHotkey::Off));
    CHECK_FALSE(matchesFlip(ctrlShift('x'), FlipHotkey::Off));
}

TEST_CASE("hotkeys are classified before the shortcut test could swallow them") {
    Settings settings; // defaults: Ctrl+` toggle, flip off
    CHECK(classify(ctrl('`'), settings) == KeyKind::Toggle);

    settings.flipHotkey = FlipHotkey::CtrlShiftZ;
    CHECK(classify(ctrlShift('z'), settings) == KeyKind::Flip);

    // With flipping off the same chord is just another Ctrl shortcut.
    settings.flipHotkey = FlipHotkey::Off;
    CHECK(classify(ctrlShift('z'), settings) == KeyKind::Shortcut);
}

TEST_CASE("a non-Shift modifier makes a shortcut, Shift alone does not") {
    Settings settings;
    CHECK(classify(ctrl('a'), settings) == KeyKind::Shortcut);

    KeyEvent alt = ascii('a');
    alt.mods.alt = true;
    CHECK(classify(alt, settings) == KeyKind::Shortcut);

    KeyEvent super = ascii('a');
    super.mods.super = true;
    CHECK(classify(super, settings) == KeyKind::Shortcut);

    // Shift is part of ordinary typing — capital letters must still compose.
    KeyEvent shifted = ascii('A');
    shifted.mods.shift = true;
    CHECK(classify(shifted, settings) == KeyKind::Compose);
}

TEST_CASE("Backspace is matched on its keysym, and Ctrl+Backspace is not") {
    Settings settings;
    CHECK(classify(bare(keysym::BackSpace), settings) == KeyKind::Backspace);

    KeyEvent chord = bare(keysym::BackSpace);
    chord.mods.ctrl = true;
    CHECK(classify(chord, settings) == KeyKind::Shortcut);
}

TEST_CASE("a key with no character is NonText") {
    Settings settings;
    CHECK(classify(bare(keysym::Return), settings) == KeyKind::NonText);
    CHECK(classify(bare(0xFF51), settings) == KeyKind::NonText); // Left arrow
}

TEST_CASE("numpad digits are told apart from top-row digits") {
    Settings settings;
    settings.method = Method::Vni;
    CHECK(classify(numpadDigit(1), settings) == KeyKind::NumpadDigit);
    // The top-row '1' is a VNI tone modifier, so it must stay composable.
    CHECK(classify(ascii('1'), settings) == KeyKind::Compose);
}

TEST_CASE("boundaries and ordinary characters") {
    Settings settings;
    settings.method = Method::Telex;
    CHECK(classify(ascii(' '), settings) == KeyKind::Boundary);
    CHECK(classify(ascii('.'), settings) == KeyKind::Boundary);
    CHECK(classify(ascii('['), settings) == KeyKind::Boundary);
    CHECK(classify(ascii('a'), settings) == KeyKind::Compose);

    // The method reaches classify through isBoundary.
    settings.method = Method::TelexAdvanced;
    CHECK(classify(ascii('['), settings) == KeyKind::Compose);
}
