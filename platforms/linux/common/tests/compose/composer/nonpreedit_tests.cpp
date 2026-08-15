// Non-preedit mode: the word is built in the document, not in a preedit.
//
// The contract is a single sentence — the same keys must leave the same text on
// screen in either mode — so most of these compare the two documents rather than
// assert plan-by-plan, which would only restate the implementation.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("both modes leave the same document") {
    // A tone applied retroactively, a boundary, an English word the engine restores,
    // and a syllable whose characters are multi-byte.
    for (const std::string keys : {"tieengs", "tieengs ", "card ", "nghieengs "}) {
        CAPTURE(keys);
        CHECK(nonPreeditDocument(Method::Telex, keys) ==
              preeditDocument(Method::Telex, keys));
    }
    CHECK(nonPreeditDocument(Method::Vni, "tieng61 ") ==
          preeditDocument(Method::Vni, "tieng61 "));
}

TEST_CASE("the word reaches the document without a preedit") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    std::string document;
    for (char c : std::string("tieengs")) {
        const ComposePlan plan = composer.onKey(ascii(c));
        // The point of the mode: nothing is ever parked in a preedit.
        CHECK(plan.effect != Effect::Preedit);
        applyPlan(document, plan, c);
    }
    CHECK(document == "tiếng");
}

TEST_CASE("deleteChars counts characters, not bytes") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    std::string document;
    for (char c : std::string("tieeng")) applyPlan(document, composer.onKey(ascii(c)), c);
    REQUIRE(document == "tiêng"); // 5 characters, 7 bytes

    // The tone key rewrites the tail: `ê` becomes `ế`, both three bytes. The repair
    // is applied here as a character count — reading it as bytes would delete past
    // the vowel and leave a truncated word.
    const ComposePlan tone = composer.onKey(ascii('s'));
    CHECK(tone.effect == Effect::Replace);
    CHECK(tone.consumed);
    // Three characters come off (`êng`, five bytes) and `ếng` goes on. Pinned the
    // way funput-desktop's inject tests pin theirs: it is one engine contract across
    // the platforms, so a change to this number is a change everywhere.
    CHECK(tone.deleteChars == 3);
    applyPlan(document, tone, 's');
    CHECK(document == "tiếng");
}

TEST_CASE("a word boundary lands exactly once") {
    CHECK(nonPreeditDocument(Method::Telex, "tieengs ") == "tiếng ");
}

TEST_CASE("an invalid Vietnamese word is restored to the raw keys") {
    CHECK(nonPreeditDocument(Method::Telex, "card ") == "card ");
}

TEST_CASE("backspace lets the app delete its own character") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    type(composer, "tieengs");

    const ComposePlan plan = composer.onKey(bare(keysym::BackSpace));
    // The character is in the document, so the app removes it; the composer only
    // keeps the engine in step. Swallowing the key would leave it on screen.
    CHECK(plan.isNoop());
}

TEST_CASE("flushing does not type the word a second time") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    type(composer, "tieengs");

    const ComposePlan plan = composer.flush();
    CHECK(plan.effect == Effect::None);
    CHECK(plan.text.empty());

    // The composition is over either way: a second flush still yields nothing.
    CHECK(composer.flush().effect == Effect::None);
}

TEST_CASE("Enter ends the composition without re-typing it") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    type(composer, "tieengs");

    const ComposePlan plan = composer.onKey(bare(keysym::Return));
    CHECK(plan.effect == Effect::None);
    CHECK(!plan.consumed); // the app still has to see the Enter
}

TEST_CASE("the mode is off unless it is asked for") {
    Composer composer = composerFor(Method::Telex);
    CHECK(!composer.nonPreedit());
    // Byte-for-byte the behaviour of every other test in this directory.
    CHECK(composer.onKey(ascii('t')).effect == Effect::Preedit);
    CHECK(type(composer, "ieengs").text == "tiếng");
}

TEST_CASE("the setting decides the mode") {
    Settings settings;
    settings.method = Method::Telex;
    settings.nonPreedit = true;

    Composer composer(settings);
    CHECK(composer.nonPreedit());
    CHECK(composer.onKey(ascii('t')).effect != Effect::Preedit);
}

TEST_CASE("changing the setting drops the half-typed word") {
    Settings settings;
    settings.method = Method::Telex;
    settings.nonPreedit = true;
    Composer composer(settings);
    type(composer, "tieeng");

    // Switching mid-word: the document holds `tiêng` and the preedit path knows
    // nothing about it, so the composition has to go rather than be handed over.
    composer.settings().nonPreedit = false;
    composer.applySettings();
    CHECK(!composer.nonPreedit());
    CHECK(composer.flush().text.empty());
}
