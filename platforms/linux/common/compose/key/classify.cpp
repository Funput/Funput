#include "compose/key/classify.h"

#include "compose/key/boundary.h"

namespace funput {
namespace {

bool exact(const Mods &m, bool ctrl, bool alt, bool super, bool shift) {
    return m.ctrl == ctrl && m.alt == alt && m.super == super && m.shift == shift;
}

// The X11 keysyms of keys that only change what the next key means. Both shells send
// their presses to the engine, and none of them produces a character.
bool isModifierKeysym(uint32_t sym) {
    // Shift, Control, Caps Lock, Shift Lock, Meta, Alt, Super, Hyper (left and right).
    if (sym >= 0xFFE1 && sym <= 0xFFEE) return true;
    switch (sym) {
    case 0xFE03: // ISO_Level3_Shift, which is AltGr
    case 0xFE11: // ISO_Level5_Shift
    case 0xFF7E: // Mode_switch
    case 0xFF7F: // Num_Lock
        return true;
    default:
        return false;
    }
}

} // namespace

bool matchesToggle(const KeyEvent &ev, Hotkey preset) {
    switch (preset) {
    case Hotkey::CtrlBacktick:
        return exact(ev.mods, true, false, false, false) && ev.keysym == keysym::Grave;
    case Hotkey::CtrlSpace:
        return exact(ev.mods, true, false, false, false) && ev.keysym == keysym::Space;
    case Hotkey::SuperSpace:
        return exact(ev.mods, false, false, true, false) && ev.keysym == keysym::Space;
    case Hotkey::CtrlShiftSpace:
        return exact(ev.mods, true, false, false, true) && ev.keysym == keysym::Space;
    // Modifier-only: both shells resolve this on release via ToggleChord.
    case Hotkey::AltShift:
        return false;
    }
    return false;
}

bool matchesFlip(const KeyEvent &ev, FlipHotkey preset) {
    if (!ev.mods.ctrl || !ev.mods.shift) return false;
    switch (preset) {
    case FlipHotkey::CtrlShiftZ:
        return ev.keysym == keysym::LowerZ || ev.keysym == keysym::UpperZ;
    case FlipHotkey::CtrlShiftX:
        return ev.keysym == keysym::LowerX || ev.keysym == keysym::UpperX;
    case FlipHotkey::Off: return false;
    }
    return false;
}

KeyKind classify(const KeyEvent &ev, const Settings &settings) {
    if (isModifierKeysym(ev.keysym)) return KeyKind::Modifier;
    // Both hotkeys hold a non-Shift modifier, so they must be matched before the
    // Shortcut test would swallow them.
    if (matchesToggle(ev, settings.toggleHotkey)) return KeyKind::Toggle;
    if (matchesFlip(ev, settings.flipHotkey)) return KeyKind::Flip;
    if (ev.mods.isShortcut()) return KeyKind::Shortcut;
    // Matched on the keysym, before `ch` is consulted: Ctrl+BackSpace has already
    // been claimed as a Shortcut above, and BackSpace's keysym-to-Unicode mapping
    // is not something the typing rules should depend on.
    if (ev.keysym == keysym::BackSpace) return KeyKind::Backspace;
    // A control character is a key, not text. Both frameworks hand one over for the
    // keys that used to have an ASCII meaning — `keySymToUnicode` was measured
    // answering CR for Return, HT for Tab and ESC for Escape — so testing `ch == 0`
    // alone let Enter fall through to the boundary test below, which counts CR as a
    // word boundary. That swallowed the key and appended the CR to the commit: the app
    // never saw Enter and got a newline inside a commit string instead. Arrows and
    // F-keys arrive with ch == 0 and are the same kind of key, which is why they share
    // this line rather than being tested apart.
    if (ev.ch < 0x20 || ev.ch == 0x7F) return KeyKind::NonText;
    if (isNumpadDigitKeysym(ev.keysym)) return KeyKind::NumpadDigit;
    if (isBoundary(ev.ch, settings.method)) return KeyKind::Boundary;
    return KeyKind::Compose;
}

} // namespace funput
