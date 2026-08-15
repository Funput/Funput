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

// --- the document a run would leave behind ------------------------------------
//
// The two modes differ in *when* text reaches the client, so comparing them plan by
// plan proves nothing; comparing what the user ends up looking at proves everything.
// These apply a plan the way a shell does. Printable keys only — a plan the app
// receives is assumed to type its own character.

// Delete `count` characters from the end. UTF-8 aware on purpose: `deleteChars` is a
// character count, and treating it as bytes is the failure this guards against.
inline void popChars(std::string &text, uint32_t count) {
    for (uint32_t i = 0; i < count && !text.empty(); ++i) {
        size_t at = text.size() - 1;
        while (at > 0 && (static_cast<unsigned char>(text[at]) & 0xC0) == 0x80) --at;
        text.erase(at);
    }
}

inline void applyPlan(std::string &document, const ComposePlan &plan, char key) {
    switch (plan.effect) {
    case Effect::None:
    case Effect::Preedit: // not in the document until it commits
        break;
    case Effect::Commit:
        document += plan.text;
        break;
    case Effect::Replace:
        popChars(document, plan.deleteChars);
        document += plan.text;
        break;
    }
    if (!plan.consumed) document += key; // the app types the key itself
}

// Type `keys` and return the resulting document, flushing at the end: a preedit run
// still holds the last word, a non-preedit run wrote it as it went.
inline std::string typeDocument(Composer &composer, const std::string &keys) {
    std::string document;
    for (char c : keys) applyPlan(document, composer.onKey(ascii(c)), c);
    const ComposePlan tail = composer.flush();
    if (tail.effect == Effect::Commit) document += tail.text;
    return document;
}

// A composer configured for one input method, with everything else default.
inline Composer composerFor(Method method) {
    Settings settings;
    settings.method = method;
    return Composer(settings);
}

// The same keys through a fresh composer in each mode. Equal documents is the
// contract non-preedit has to keep.
inline std::string preeditDocument(Method method, const std::string &keys) {
    Composer composer = composerFor(method);
    return typeDocument(composer, keys);
}

inline std::string nonPreeditDocument(Method method, const std::string &keys) {
    Composer composer = composerFor(method);
    composer.setNonPreedit(true);
    return typeDocument(composer, keys);
}

} // namespace funput::test

#endif // FUNPUT_TESTS_SUPPORT_H
