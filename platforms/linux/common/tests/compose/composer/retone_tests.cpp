// Re-opening a finished word after Backspace, so its tone can still be fixed.
//
// The Linux counterpart of crates/funput-desktop/src/retone/tests.rs. The rule is
// the same on both, and so now is where the word comes from: a shadow copy of what
// was typed — the hook shell's because it has no document to read, non-preedit's
// because the document it reads may be a keystroke old. Which word the scan picks:
// retone/scan_tests.cpp.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/refusal.h"
#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("backspace re-opens the finished word so its tone can be fixed") {
    ClientSim sim;
    // `phủ ` is in the document and the Backspace deletes the trailing space.
    REQUIRE(sim.type("phur ") == "phủ ");
    sim.pressBackspace();
    REQUIRE(sim.adopted());
    REQUIRE(sim.document() == "phủ");

    const ComposePlan plan = sim.press(ascii('s'));
    CHECK(plan.effect == Effect::Replace);
    CHECK(sim.document() == "phú");
}

TEST_CASE("a client that only drops the repair after a re-open keeps the mode") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // Chrome's address bar: ordinary repairs land, so type a word the honest way.
    std::string document;
    for (char c : std::string("tieengs ")) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    REQUIRE(document == "tiếng ");

    // Backspace over the space, re-open the word, then let the repair that follows be
    // dropped — which is the one that client discards.
    composer.observeDocument(document);
    applyPlan(document, composer.onKey(bare(keysym::BackSpace)), '\b');
    REQUIRE(composer.adoptWordBeforeBackspace());
    REQUIRE(document == "tiếng");

    composer.observeDocument(document);
    applyIgnoringDeletes(document, composer.onKey(ascii('f')), 'f');
    composer.observeDocument(document);

    // Only re-toning is given up. Typing straight into the document still works here,
    // and throwing that away too would cost more than the failure did.
    CHECK(composer.nonPreedit());
    composer.observeDocument(document);
    composer.onKey(bare(keysym::BackSpace));
    CHECK_FALSE(composer.adoptWordBeforeBackspace());
}

TEST_CASE("non-preedit does its own deleting on Backspace") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // Type the word first: the takeover needs positive evidence that the document
    // being read is current, and a repair seen to land is that evidence.
    std::string document;
    for (char c : std::string("tieengs ")) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);

    // Letting the app delete is what breaks re-toning on Chrome's address bar: it
    // edits behind our back and then discards the repair that follows. Keeping the
    // deletion on the same channel as every other repair keeps the whole word on a
    // path the client has already shown it will follow.
    const ComposePlan plan = composer.onKey(bare(keysym::BackSpace));
    CHECK(plan.effect == Effect::Replace);
    CHECK(plan.deleteChars == 1);
    CHECK(plan.text.empty());
    CHECK(plan.consumed);
}

TEST_CASE("a word is re-opened only over the Backspace that was just journaled") {
    // The offer is tied to the key `onKey()` just handled. Asked again for the same
    // Backspace, or after a reading has moved the journal on, there is nothing to
    // re-open — a shell cannot adopt twice, and a test cannot adopt out of order.
    ClientSim sim;
    sim.type("phur ");
    sim.pressBackspace();
    REQUIRE(sim.adopted());
    CHECK_FALSE(sim.composer().adoptWordBeforeBackspace());
}

TEST_CASE("a live selection keeps its Backspace") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    std::string document;
    for (char c : std::string("tieengs ")) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document, /*selectionLive=*/true);

    // The user pressed Backspace to delete the selection. Swallowing the key to run a
    // one-character delete would both lose that and eat the wrong text.
    CHECK(composer.onKey(bare(keysym::BackSpace)).isNoop());
}

TEST_CASE("a preedit shell still lets the app delete") {
    Composer composer = composerFor(Method::Telex);
    composer.observeDocument("tiếng ");
    CHECK(composer.onKey(bare(keysym::BackSpace)).isNoop());
}
