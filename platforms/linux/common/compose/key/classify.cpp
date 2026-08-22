#include "compose/key/classify.h"

#include "compose/key/boundary.h"

namespace funput {
namespace {

bool exact(const Mods &m, bool ctrl, bool alt, bool super, bool shift) {
    return m.ctrl == ctrl && m.alt == alt && m.super == super && m.shift == shift;
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
    // Both hotkeys hold a non-Shift modifier, so they must be matched before the
    // Shortcut test would swallow them.
    if (matchesToggle(ev, settings.toggleHotkey)) return KeyKind::Toggle;
    if (matchesFlip(ev, settings.flipHotkey)) return KeyKind::Flip;
    if (ev.mods.isShortcut()) return KeyKind::Shortcut;
    // Matched on the keysym, before `ch` is consulted: Ctrl+BackSpace has already
    // been claimed as a Shortcut above, and BackSpace's keysym-to-Unicode mapping
    // is not something the typing rules should depend on.
    if (ev.keysym == keysym::BackSpace) return KeyKind::Backspace;
    if (ev.ch == 0) return KeyKind::NonText;
    if (isNumpadDigitKeysym(ev.keysym)) return KeyKind::NumpadDigit;
    if (isBoundary(ev.ch, settings.method)) return KeyKind::Boundary;
    return KeyKind::Compose;
}

} // namespace funput
