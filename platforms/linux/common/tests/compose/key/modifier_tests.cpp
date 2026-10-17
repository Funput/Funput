// A modifier key's own press, as the shells deliver it. Ctrl+Shift+Z is three presses
// and the first two arrive before the Z, so what they classify as decides whether the
// word is still there when the flip hotkey runs.

#include <doctest/doctest.h>

#include "compose/key/chord.h"
#include "compose/key/classify.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

// A key with a keysym and no character, which is how both frameworks report a modifier.
KeyEvent modifier(uint32_t sym, Mods held = {}) {
    KeyEvent ev;
    ev.keysym = sym;
    ev.mods = held;
    return ev;
}

Settings withFlip() {
    Settings settings;
    settings.flipHotkey = FlipHotkey::CtrlShiftZ;
    return settings;
}

} // namespace

TEST_CASE("a modifier press is a modifier whatever is already held") {
    const Settings settings = withFlip();
    Mods ctrlHeld;
    ctrlHeld.ctrl = true;
    Mods both;
    both.ctrl = both.shift = true;

    for (uint32_t sym : {keysym::ShiftL, keysym::ShiftR, keysym::CtrlL, keysym::CtrlR,
                         keysym::AltL, keysym::AltR, keysym::SuperL, keysym::SuperR}) {
        CHECK(classify(modifier(sym), settings) == KeyKind::Modifier);
        CHECK(classify(modifier(sym, ctrlHeld), settings) == KeyKind::Modifier);
        CHECK(classify(modifier(sym, both), settings) == KeyKind::Modifier);
    }
}

TEST_CASE("the locks, AltGr and Mode_switch are modifiers too") {
    const Settings settings;
    for (uint32_t sym : {0xFFE5u /* Caps_Lock */, 0xFFE6u /* Shift_Lock */,
                         0xFFE7u /* Meta_L */, 0xFFEDu /* Hyper_L */,
                         0xFE03u /* ISO_Level3_Shift, AltGr */, 0xFF7Eu /* Mode_switch */,
                         0xFF7Fu /* Num_Lock */}) {
        CHECK(classify(modifier(sym), settings) == KeyKind::Modifier);
    }
}

TEST_CASE("keys next to the modifier range are not modifiers") {
    const Settings settings;
    CHECK(classify(modifier(0xFFE0), settings) != KeyKind::Modifier);
    CHECK(classify(modifier(0xFFEF), settings) != KeyKind::Modifier);
    CHECK(classify(modifier(keysym::BackSpace), settings) == KeyKind::Backspace);
}

TEST_CASE("the chords that held a modifier keep their meaning") {
    const Settings settings = withFlip();
    CHECK(classify(ctrlShift('z'), settings) == KeyKind::Flip);
    CHECK(classify(ctrl('a'), settings) == KeyKind::Shortcut);
    CHECK(classify(ctrl('`'), settings) == KeyKind::Toggle);
    CHECK(classify(ascii('a'), settings) == KeyKind::Compose);
}
