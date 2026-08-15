// What a keystroke means for composition: the rules that read a [KeyEvent].

#ifndef FUNPUT_COMPOSE_KEY_CLASSIFY_H
#define FUNPUT_COMPOSE_KEY_CLASSIFY_H

#include "compose/key/event.h"
#include "settings/settings.h"

namespace funput {

// The order the composer tests these in is the order `classify` returns them —
// see the comment there.
enum class KeyKind {
    Toggle,      // the VI/EN hotkey
    Flip,        // the flip-composing (VN <-> raw keys) hotkey
    Shortcut,    // Ctrl/Alt/Super held: flush and let the app have the key
    Backspace,   // shorten the composition
    NonText,     // produces no character: flush and pass through
    NumpadDigit, // a literal number, never a VNI tone/shape modifier
    Boundary,    // ends the word: commit it along with this character
    Compose,     // an ordinary character to feed the engine
};

// Classify one key against the user's settings. Pure: no engine state is consulted,
// so the VI/EN gate lives in the composer, between `Toggle` and everything else —
// exactly where both shells put it today.
//
// Toggle and Flip come first because their combos hold Ctrl (and Shift), which
// would otherwise be swallowed by the `Shortcut` test. `Backspace` is matched on
// its keysym before `ch` is looked at, so Ctrl+BackSpace stays a `Shortcut`.
KeyKind classify(const KeyEvent &ev, const Settings &settings);

// Whether `ev` is the configured VI/EN toggle. Hotkey::AltShift has no keysym form
// here and never matches — the shells do not implement it either.
bool matchesToggle(const KeyEvent &ev, Hotkey preset);

// Whether `ev` is the configured flip-composing hotkey (a Ctrl+Shift+<letter>
// preset, or FlipHotkey::Off which never matches).
bool matchesFlip(const KeyEvent &ev, FlipHotkey preset);

} // namespace funput

#endif // FUNPUT_COMPOSE_KEY_CLASSIFY_H
