// Re-opening a finished word after Backspace, so its tone can still be fixed.
//
// The Linux counterpart of crates/funput-desktop/src/retone/tests.rs. The rule is
// the same on both; what differs is where the word comes from — a hook shell keeps a
// shadow copy of what it typed, while non-preedit here can just read the document.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

// A client that takes the commit and drops the delete beside it.
void applyIgnoringDeletes(std::string &document, const ComposePlan &plan, char key) {
    if (plan.effect != Effect::Replace) {
        applyPlan(document, plan, key);
        return;
    }
    document += plan.text;
}

// Type `keys` into a client that answers every keystroke on time, the way a shell
// drives the composer: read the document, check it, then press the key. Ends with a
// reading taken, so the next Backspace has the proof `adoptWordBeforeBackspace` asks
// for — that the document it reads is the one the last repair left.
std::string typeInSync(Composer &composer, const std::string &keys) {
    std::string document;
    for (char c : keys) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);
    return document;
}

// One Backspace the way a shell handles it: the reading it took before the key is
// the word it offers for re-opening afterwards. Returns whether a word was re-opened.
bool backspace(Composer &composer, std::string &document) {
    const std::string reading = document;
    const ComposePlan plan = composer.onKey(bare(keysym::BackSpace));
    if (plan.effect == Effect::Replace) {
        popChars(document, plan.deleteChars);
    } else if (!plan.consumed) {
        popChars(document, 1); // the app deletes its own character
    }
    const bool adopted = composer.adoptWordBeforeBackspace(reading);
    composer.observeDocument(document);
    return adopted;
}

} // namespace

TEST_CASE("backspace re-opens the finished word so its tone can be fixed") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    // `phủ ` is in the document and the app is about to delete the trailing space.
    REQUIRE(typeInSync(composer, "phur ") == "phủ ");
    REQUIRE(composer.adoptWordBeforeBackspace("phủ "));

    std::string document = "phủ"; // what the app leaves once it has deleted
    const ComposePlan plan = composer.onKey(ascii('s'));
    CHECK(plan.effect == Effect::Replace);
    applyPlan(document, plan, 's');
    CHECK(document == "phú");
}

TEST_CASE("only a Vietnamese syllable is re-opened") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    // English words stay literal — the engine refuses them.
    CHECK_FALSE(composer.adoptWordBeforeBackspace("hello "));
    // The caret lands on a separator, not on a word.
    CHECK_FALSE(composer.adoptWordBeforeBackspace("phủ, "));
    // Nothing in front of the caret at all.
    CHECK_FALSE(composer.adoptWordBeforeBackspace(""));
}

TEST_CASE("the word scan splits on punctuation, as the hook shells do") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    // `github.com` is not one word here: the scan stops at the dot and offers `com`,
    // which really is a Vietnamese syllable, so it is re-opened. `CommittedTail`'s
    // `is_separator` splits the same way on Windows. Pinned because it is a decision
    // shared with the other platforms, not an accident of this one.
    REQUIRE(typeInSync(composer, "github.com ") == "github.com ");
    CHECK(composer.adoptWordBeforeBackspace("github.com "));
}

// `gõ` + `x` restores `gox`; Space, then ⌫⌫ deletes the space and the `x`, and `x`
// again must put the tone back. The first Backspace was taken over but not recorded,
// so the second had no proof the document was current and went to the app — and the
// repair after an app-handled Backspace is the one Chrome drops. `go` + `õ`: `goõ`.
TEST_CASE("consecutive Backspaces all stay on Funput's channel") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    std::string document = typeInSync(composer, "goxx ");
    REQUIRE(document == "gox ");

    CHECK_FALSE(backspace(composer, document)); // `gox` is not a syllable
    REQUIRE(document == "gox");

    // Taken over too, not handed to the app: the first delete was seen to land.
    const ComposePlan second = composer.onKey(bare(keysym::BackSpace));
    CHECK(second.effect == Effect::Replace);
    CHECK(second.deleteChars == 1);
    CHECK(second.consumed);
    popChars(document, 1);
    REQUIRE(composer.adoptWordBeforeBackspace("gox"));
    composer.observeDocument(document);
    REQUIRE(document == "go");

    applyPlan(document, composer.onKey(ascii('x')), 'x');
    CHECK(document == "gõ");
}

// Chrome reports surrounding text late, so the reading a Backspace gets can be the
// document from a keystroke ago. After Space the stale copy still reads `gox`, and
// dropping the character the app "is about to delete" offered `go` — a word the
// document no longer ends with. The next `x` then wrote `goõ`.
TEST_CASE("a stale reading of the document re-opens nothing") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    std::string document;
    std::string reading; // what the client last reported: one keystroke behind
    for (char c : std::string("goxx ")) {
        composer.observeDocument(reading);
        reading = document;
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    REQUIRE(document == "gox ");

    composer.observeDocument(reading); // still `gox`: the space has not been reported
    REQUIRE(reading == "gox");
    const ComposePlan plan = composer.onKey(bare(keysym::BackSpace));
    CHECK_FALSE(plan.consumed); // no proof the reading is current, so the app deletes
    popChars(document, 1);
    CHECK_FALSE(composer.adoptWordBeforeBackspace(reading));

    composer.observeDocument(reading);
    applyPlan(document, composer.onKey(ascii('x')), 'x');
    CHECK(document == "goxx");
}

TEST_CASE("re-opening is a non-preedit affair only") {
    Composer composer = composerFor(Method::Telex);
    // A preedit shell has no committed word to re-open; Backspace shortens the
    // composition instead.
    CHECK_FALSE(composer.adoptWordBeforeBackspace("phủ "));
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
    REQUIRE(composer.adoptWordBeforeBackspace("tiếng "));
    REQUIRE(document == "tiếng");

    composer.observeDocument(document);
    applyIgnoringDeletes(document, composer.onKey(ascii('f')), 'f');
    composer.observeDocument(document);

    // Only re-toning is given up. Typing straight into the document still works here,
    // and throwing that away too would cost more than the failure did.
    CHECK(composer.nonPreedit());
    CHECK_FALSE(composer.adoptWordBeforeBackspace("tiếng "));
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
