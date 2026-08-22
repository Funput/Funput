#include <doctest/doctest.h>

#include "compose/key/chord.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

KeyEvent modDown(uint32_t sym, Mods held) {
    KeyEvent ev;
    ev.keysym = sym;
    ev.mods = held;
    return ev;
}

} // namespace

TEST_CASE("key-down presets still fire through ToggleChord") {
    ToggleChord chord;
    CHECK(chord.feed(ctrl('`'), false, Hotkey::CtrlBacktick));
    CHECK_FALSE(chord.feed(ctrl('`'), true, Hotkey::CtrlBacktick));
    CHECK_FALSE(chord.feed(ascii('`'), false, Hotkey::CtrlBacktick));

    KeyEvent superSpace = ascii(' ');
    superSpace.mods.super = true;
    CHECK(chord.feed(superSpace, false, Hotkey::SuperSpace));
    CHECK(chord.feed(ctrlShift(' '), false, Hotkey::CtrlShiftSpace));
}

TEST_CASE("Alt+Shift fires on the first modifier release") {
    ToggleChord chord;
    Mods held;
    held.alt = true;
    CHECK_FALSE(chord.feed(modDown(keysym::AltL, held), false, Hotkey::AltShift));
    held.shift = true;
    CHECK_FALSE(chord.feed(modDown(keysym::ShiftL, held), false, Hotkey::AltShift));
    held.shift = false;
    CHECK(chord.feed(modDown(keysym::ShiftL, held), true, Hotkey::AltShift));
}

TEST_CASE("Alt+Shift+Tab does not toggle") {
    ToggleChord chord;
    Mods held;
    held.alt = true;
    CHECK_FALSE(chord.feed(modDown(keysym::AltL, held), false, Hotkey::AltShift));
    held.shift = true;
    CHECK_FALSE(chord.feed(modDown(keysym::ShiftL, held), false, Hotkey::AltShift));
    KeyEvent tab = ascii('\t');
    tab.mods = held;
    CHECK_FALSE(chord.feed(tab, false, Hotkey::AltShift));
    held.shift = false;
    CHECK_FALSE(chord.feed(modDown(keysym::ShiftL, held), true, Hotkey::AltShift));
}

TEST_CASE("holding Alt and tapping Shift toggles each tap") {
    ToggleChord chord;
    Mods held;
    held.alt = true;
    CHECK_FALSE(chord.feed(modDown(keysym::AltL, held), false, Hotkey::AltShift));
    held.shift = true;
    CHECK_FALSE(chord.feed(modDown(keysym::ShiftL, held), false, Hotkey::AltShift));
    held.shift = false;
    CHECK(chord.feed(modDown(keysym::ShiftL, held), true, Hotkey::AltShift));
    held.shift = true;
    CHECK_FALSE(chord.feed(modDown(keysym::ShiftL, held), false, Hotkey::AltShift));
    held.shift = false;
    CHECK(chord.feed(modDown(keysym::ShiftL, held), true, Hotkey::AltShift));
}

TEST_CASE("extra modifiers in the peak do not fire") {
    ToggleChord chord;
    Mods held;
    held.ctrl = true;
    held.alt = true;
    CHECK_FALSE(chord.feed(modDown(keysym::AltL, held), false, Hotkey::AltShift));
    held.shift = true;
    CHECK_FALSE(chord.feed(modDown(keysym::ShiftL, held), false, Hotkey::AltShift));
    held.shift = false;
    CHECK_FALSE(chord.feed(modDown(keysym::ShiftL, held), true, Hotkey::AltShift));
}
