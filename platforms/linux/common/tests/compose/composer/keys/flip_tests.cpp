// The flip hotkey as a user presses it: Ctrl down, Shift down, then the letter. The
// keys.cpp tests send the letter alone, which both shells never do — and it hid that
// the two modifier presses ended the word before the letter could flip it.

#include <doctest/doctest.h>

#include <string>

#include "compose/key/chord.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

Composer telex(bool nonPreedit) {
    Settings settings;
    settings.method = Method::Telex;
    settings.flipHotkey = FlipHotkey::CtrlShiftZ;
    settings.nonPreedit = nonPreedit;
    return Composer(settings);
}

// A modifier's press: a keysym and no character.
ComposePlan press(Composer &composer, uint32_t sym, Mods held) {
    KeyEvent ev;
    ev.keysym = sym;
    ev.mods = held;
    return composer.onKey(ev);
}

// Ctrl down, Shift down, Z — and what the Z did. Both modifier presses must go
// through untouched, which is what keeps the word there for the Z.
ComposePlan flipChord(Composer &composer) {
    Mods none;
    Mods ctrlHeld;
    ctrlHeld.ctrl = true;
    CHECK(press(composer, keysym::CtrlL, none).isNoop());
    CHECK(press(composer, keysym::ShiftL, ctrlHeld).isNoop());
    return composer.onKey(ctrlShift('Z'));
}

void feed(Composer &composer, std::string &document, const std::string &keys) {
    for (char c : keys) applyPlan(document, composer.onKey(ascii(c)), c);
}

} // namespace

TEST_CASE("the flip chord swaps a preedit word for its raw keys and back") {
    Composer composer = telex(false);
    type(composer, "tieengs");

    const ComposePlan raw = flipChord(composer);
    CHECK(raw.effect == Effect::Preedit);
    CHECK(raw.text == "tieengs");
    CHECK(raw.consumed);

    const ComposePlan vietnamese = flipChord(composer);
    CHECK(vietnamese.effect == Effect::Preedit);
    CHECK(vietnamese.text == "tiếng");
}

TEST_CASE("the flip chord rewrites a word typed straight into the document") {
    Composer composer = telex(true);
    std::string document;
    feed(composer, document, "tieengs");
    REQUIRE(document == "tiếng");

    applyPlan(document, flipChord(composer), 'Z');
    CHECK(document == "tieengs");

    applyPlan(document, flipChord(composer), 'Z');
    CHECK(document == "tiếng");
}

TEST_CASE("a lone Shift press in the middle of a word does not end it") {
    for (bool nonPreedit : {false, true}) {
        Composer composer = telex(nonPreedit);
        std::string document;
        feed(composer, document, "tie");

        CHECK(press(composer, keysym::ShiftL, Mods{}).isNoop());
        feed(composer, document, "engs");

        const ComposePlan tail = composer.flush();
        if (tail.effect == Effect::Commit) document += tail.text;
        CHECK(document == "tiếng");
    }
}
