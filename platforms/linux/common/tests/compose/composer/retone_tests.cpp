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

} // namespace

TEST_CASE("backspace re-opens the finished word so its tone can be fixed") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    // `phủ ` is in the document and the app is about to delete the trailing space.
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
    CHECK(composer.adoptWordBeforeBackspace("github.com "));
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
    composer.onKey(bare(keysym::BackSpace));
    REQUIRE(composer.adoptWordBeforeBackspace(document));
    document = "tiếng";

    composer.observeDocument(document);
    applyIgnoringDeletes(document, composer.onKey(ascii('f')), 'f');
    composer.observeDocument(document);

    // Only re-toning is given up. Typing straight into the document still works here,
    // and throwing that away too would cost more than the failure did.
    CHECK(composer.nonPreedit());
    CHECK_FALSE(composer.adoptWordBeforeBackspace("tiếng "));
}
