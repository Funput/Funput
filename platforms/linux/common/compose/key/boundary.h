// Word-boundary test shared by the Linux input-method shells (Fcitx5, IBus).
// A boundary ends the composing word and triggers commit / English-restore.

#ifndef FUNPUT_BOUNDARY_H
#define FUNPUT_BOUNDARY_H

#include <cstdint>

#include "settings/settings.h"

namespace funput {

// ASCII whitespace or punctuation; digits excluded (VNI uses them as tone
// modifiers). Mirrors funput_core's rule and the macOS shell.
inline bool isBoundary(char32_t s, Method method) {
    if (s > 0x7F) return false;
    if ((s == U'[' || s == U']') && method == Method::TelexAdvanced) return false;
    if (s == U' ' || s == U'\t' || s == U'\n' || s == U'\r') return true;
    const uint32_t v = static_cast<uint32_t>(s);
    return (v >= 0x21 && v <= 0x2F) || (v >= 0x3A && v <= 0x40) ||
           (v >= 0x5B && v <= 0x60) || (v >= 0x7B && v <= 0x7E);
}

// A numeric-keypad digit, identified by its X11 keysym KP_0..KP_9 (0xff80+0x30 =
// 0xffb0 .. 0xffb9). Shared by Fcitx5 (FcitxKey_KP_0) and IBus (IBUS_KEY_KP_0),
// whose keysym values are identical. These only reach the shell as digits while
// NumLock is on; NumLock off turns them into arrows/Home/End (keysym→unicode 0).
// A numpad digit must stay a literal number, so the shell tags it KeySource::Numpad.
inline bool isNumpadDigitKeysym(uint32_t keysym) { return keysym >= 0xffb0 && keysym <= 0xffb9; }

} // namespace funput

#endif // FUNPUT_BOUNDARY_H
