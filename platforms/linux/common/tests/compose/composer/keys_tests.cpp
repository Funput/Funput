// The typing baseline: every branch of the decision tree the two shells used to
// carry a copy of each. Expected values are what the shells produce today — a
// change here means the refactor changed behaviour.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("Telex composes a word one preedit at a time") {
    Composer composer = composerFor(Method::Telex);
    CHECK(composer.onKey(ascii('t')).text == "t");
    CHECK(composer.onKey(ascii('i')).text == "ti");
    CHECK(composer.onKey(ascii('e')).text == "tie");
    CHECK(composer.onKey(ascii('e')).text == "tiê"); // doubled vowel -> circumflex
    CHECK(composer.onKey(ascii('n')).text == "tiên");
    CHECK(composer.onKey(ascii('g')).text == "tiêng");

    const ComposePlan tone = composer.onKey(ascii('s')); // retro-applies the tone
    CHECK(tone.effect == Effect::Preedit);
    CHECK(tone.text == "tiếng");
    CHECK(tone.consumed);
}

TEST_CASE("VNI composes the same word with digit modifiers") {
    Composer composer = composerFor(Method::Vni);
    CHECK(type(composer, "tieng6").text == "tiêng");
    CHECK(type(composer, "1").text == "tiếng");
}

TEST_CASE("a word boundary commits the word and the boundary together") {
    Composer composer = composerFor(Method::Telex);
    type(composer, "tieengs");

    const ComposePlan plan = composer.onKey(ascii(' '));
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text == "tiếng "); // exactly one copy of the boundary
    CHECK(plan.consumed);

    // The composition is over, so there is nothing left to flush.
    CHECK(composer.flush().text.empty());
}

TEST_CASE("a boundary with nothing composing passes through") {
    Composer composer = composerFor(Method::Telex);
    const ComposePlan plan = composer.onKey(ascii(' '));
    CHECK(plan.isNoop());
}

TEST_CASE("an invalid Vietnamese word is restored to the raw keys") {
    Composer composer = composerFor(Method::Telex);
    CHECK(type(composer, "car").text == "cả"); // 'r' looks like a tone…
    const ComposePlan plan = composer.onKey(ascii('d'));
    CHECK(plan.text == "card"); // …until 'd' proves the word is English

    CHECK(composer.onKey(ascii(' ')).text == "card ");
}

TEST_CASE("Backspace shortens the composition, then passes through") {
    Composer composer = composerFor(Method::Telex);
    type(composer, "tieengs");

    const ComposePlan shortened = composer.onKey(bare(keysym::BackSpace));
    CHECK(shortened.effect == Effect::Preedit);
    CHECK(shortened.text == "tiến");
    CHECK(shortened.consumed);

    while (!composer.onKey(bare(keysym::BackSpace)).isNoop()) {
    }
    // With nothing composing the key is the app's, so it deletes its own character.
    CHECK(composer.onKey(bare(keysym::BackSpace)).isNoop());
}

TEST_CASE("a system shortcut commits but still reaches the app") {
    Composer composer = composerFor(Method::Telex);
    type(composer, "tieengs");

    const ComposePlan plan = composer.onKey(ctrl('a'));
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text == "tiếng");
    CHECK_FALSE(plan.consumed); // swallowing it would break Ctrl+A
}

TEST_CASE("a non-text key commits but still reaches the app") {
    Composer composer = composerFor(Method::Telex);
    type(composer, "tieengs");

    const ComposePlan plan = composer.onKey(bare(keysym::Return));
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text == "tiếng");
    CHECK_FALSE(plan.consumed);
}

TEST_CASE("a numpad digit stays a literal number under VNI") {
    Composer composer = composerFor(Method::Vni);
    type(composer, "a");

    // The top-row '1' would have made 'á'; the keypad's must not.
    const ComposePlan plan = composer.onKey(numpadDigit(1));
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text == "a1");
    CHECK(plan.consumed);
}

TEST_CASE("the flip hotkey swaps the word for its raw keys") {
    Settings settings;
    settings.method = Method::Telex;
    settings.flipHotkey = FlipHotkey::CtrlShiftZ;
    Composer composer(settings);

    type(composer, "tieengs");
    const ComposePlan flipped = composer.onKey(ctrlShift('z'));
    CHECK(flipped.effect == Effect::Preedit);
    CHECK(flipped.text == "tieengs");
    CHECK(flipped.consumed);
}

TEST_CASE("flipping with nothing composing is swallowed but changes nothing") {
    Settings settings;
    settings.flipHotkey = FlipHotkey::CtrlShiftZ;
    Composer composer(settings);

    const ComposePlan plan = composer.onKey(ctrlShift('z'));
    CHECK(plan.effect == Effect::None);
    CHECK(plan.consumed);
}
