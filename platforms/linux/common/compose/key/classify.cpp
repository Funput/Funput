#include "compose/key/classify.h"

#include "compose/key/boundary.h"

namespace funput {

bool matchesToggle(const KeyEvent &ev, Hotkey preset) {
    if (!ev.mods.ctrl) return false;
    switch (preset) {
    case Hotkey::CtrlBacktick: return ev.keysym == keysym::Grave;
    case Hotkey::CtrlSpace: return ev.keysym == keysym::Space;
    // No keysym form: neither shell implements a bare Alt+Shift chord, which needs
    // release-tracking rather than a key match.
    case Hotkey::AltShift: return false;
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
    // Both hotkeys hold Ctrl, so they must be matched before the Shortcut test
    // would swallow them.
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
