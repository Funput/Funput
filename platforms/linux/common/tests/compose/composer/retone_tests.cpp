// Re-opening a finished word after Backspace, so its tone can still be fixed.
//
// The Linux counterpart of crates/funput-desktop/src/retone/tests.rs. The rule is
// the same on both; what differs is where the word comes from — a hook shell keeps a
// shadow copy of what it typed, while non-preedit here can just read the document.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

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

