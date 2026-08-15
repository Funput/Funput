// The normalized key the composer works in — data only, no rules.
//
// Mirrors `crates/funput-desktop/src/key.rs` (the Windows shell's model) so the two
// desktop platforms stay recognisably the same shape.
//
// What makes one model serve both Linux shells: Fcitx5 and IBus both speak X11
// keysyms, with identical values (`FcitxKey_grave == IBUS_KEY_grave == 0x60`), so a
// plain `uint32_t` needs no abstraction. The one thing that genuinely differs is
// keysym -> Unicode (`fcitx::Key::keySymToUnicode` vs `ibus_keyval_to_unicode`);
// each shell does that itself and fills in `KeyEvent::ch`.
//
// This header deliberately depends on nothing — not even settings.h. The rules that
// read a key live next door in classify.h.

#ifndef FUNPUT_COMPOSE_KEY_EVENT_H
#define FUNPUT_COMPOSE_KEY_EVENT_H

#include <cstdint>

namespace funput {

// The X11 keysyms the composer names. Both shells' own constants carry these same
// values, so spelling them here keeps the composer free of framework headers.
namespace keysym {
inline constexpr uint32_t Space = 0x0020;
inline constexpr uint32_t Grave = 0x0060;
inline constexpr uint32_t UpperX = 0x0058;
inline constexpr uint32_t UpperZ = 0x005A;
inline constexpr uint32_t LowerX = 0x0078;
inline constexpr uint32_t LowerZ = 0x007A;
inline constexpr uint32_t BackSpace = 0xFF08;
inline constexpr uint32_t Return = 0xFF0D;
} // namespace keysym

// Modifiers held when a key was pressed. Shift is tracked but does *not* by itself
// mark a system shortcut — it is part of ordinary typing.
struct Mods {
    bool ctrl = false;
    bool alt = false;
    bool super = false;
    bool shift = false;

    // A non-Shift modifier is held, i.e. the key belongs to a system shortcut
    // (Ctrl+A, Alt+Tab, Super+…) and must not be composed.
    bool isShortcut() const { return ctrl || alt || super; }
};

// One key press, already normalized by the shell. Key *releases* never reach the
// composer: both shells drop them before this (`event.isRelease()` /
// `IBUS_RELEASE_MASK`), which is a framework detail rather than a typing rule.
struct KeyEvent {
    uint32_t keysym = 0;
    // The character this key produces, or 0 when it produces none (arrows, F-keys,
    // Enter). Numpad digits with NumLock on do produce one.
    char32_t ch = 0;
    Mods mods;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_KEY_EVENT_H
