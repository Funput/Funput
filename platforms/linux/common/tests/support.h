// Shared helpers for the compose tests: building the normalized key events the
// composer takes, and driving a string of them through it. Mirrors iOS's
// FunputTests/Support/.

#ifndef FUNPUT_TESTS_SUPPORT_H
#define FUNPUT_TESTS_SUPPORT_H

#include <string>

#include "compose/composer/composer.h"
#include "compose/key/classify.h"

namespace funput::test {

// A plain ASCII key with no modifiers. For printable ASCII the X11 keysym and the
// character it produces share the same value, which is why the shells can pass the
// keysym straight through.
inline KeyEvent ascii(char c) {
    KeyEvent ev;
    ev.keysym = static_cast<uint32_t>(static_cast<unsigned char>(c));
    ev.ch = static_cast<char32_t>(static_cast<unsigned char>(c));
    return ev;
}

inline KeyEvent ctrl(char c) {
    KeyEvent ev = ascii(c);
    ev.mods.ctrl = true;
    return ev;
}

inline KeyEvent ctrlShift(char c) {
    KeyEvent ev = ctrl(c);
    ev.mods.shift = true;
    return ev;
}

// A key that produces no character: arrows, Enter, F-keys, Backspace.
inline KeyEvent bare(uint32_t keysym) {
    KeyEvent ev;
    ev.keysym = keysym;
    return ev;
}

// A numeric-keypad digit with NumLock on: it does produce a character, but its
// keysym is KP_<n> rather than the top-row digit's.
inline KeyEvent numpadDigit(unsigned digit) {
    KeyEvent ev;
    ev.keysym = 0xffb0 + digit; // KP_0 + n
    ev.ch = static_cast<char32_t>(U'0' + digit);
    return ev;
}

// Feed every character of `keys` and return the last plan.
inline ComposePlan type(Composer &composer, const std::string &keys) {
    ComposePlan plan;
    for (char c : keys) plan = composer.onKey(ascii(c));
    return plan;
}

// A composer configured for one input method, with everything else default.
inline Composer composerFor(Method method) {
    Settings settings;
    settings.method = method;
    return Composer(settings);
}

} // namespace funput::test

#endif // FUNPUT_TESTS_SUPPORT_H
