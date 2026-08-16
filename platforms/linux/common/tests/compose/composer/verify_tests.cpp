// Non-preedit's check that the client is doing as it is told.
//
// Chrome's address bar takes a commit and drops the `deleteSurroundingText` beside
// it, so every repair appends instead of replacing and `phủ` becomes `phủú`. These
// drive the composer against three clients — one that obeys, one that behaves like
// that address bar, and one that simply has not answered yet — because the whole
// design rests on those three being different strings.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

// What the address bar does: the commit lands, the delete is thrown away.
void applyIgnoringDeletes(std::string &document, const ComposePlan &plan, char key) {
    if (plan.effect != Effect::Replace) {
        applyPlan(document, plan, key);
        return;
    }
    document += plan.text;
}

} // namespace

TEST_CASE("a client that obeys keeps the mode") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    std::string document;
    for (char c : std::string("tieengs ")) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);

    CHECK(composer.nonPreedit());
    CHECK(document == "tiếng ");
}

TEST_CASE("a client that ignores deletes loses the mode") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // Stop at the verdict rather than typing through it: once the mode is off the
    // remaining keys compose in a preedit like any other, which would leave a live
    // composition and say nothing about this.
    std::string document;
    for (char c : std::string("tieengs")) {
        composer.observeDocument(document);
        if (!composer.nonPreedit()) break;
        applyIgnoringDeletes(document, composer.onKey(ascii(c)), c);
    }

    CHECK_FALSE(composer.nonPreedit());
    // The composition goes with it: the engine believed it owned a word the document
    // does not have, and the next delete would aim at text Funput never wrote.
    CHECK_FALSE(composer.isComposing());
}

TEST_CASE("a silent client keeps the mode") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // The client answers nothing, so every reading is the document as it was before
    // any of this. That is the common case — 61% of commits go unanswered — and
    // reading it as failure would disable the mode for almost everybody.
    const std::string stale;
    for (char c : std::string("tieengs ")) {
        composer.observeDocument(stale);
        composer.onKey(ascii(c));
    }
    composer.observeDocument(stale);

    CHECK(composer.nonPreedit());
}

TEST_CASE("a repair with nothing to delete is not a failure") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // Early letters compose without rewriting anything, so their repairs delete zero
    // characters and the document legitimately just grows. "Applied" and "delete
    // ignored" are the same string there, which must read as no verdict rather than
    // as the client misbehaving.
    std::string document;
    for (char c : std::string("ti")) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);

    CHECK(composer.nonPreedit());
}

TEST_CASE("after the fallback the word goes back into a preedit") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    std::string document;
    for (char c : std::string("tieengs")) {
        composer.observeDocument(document);
        applyIgnoringDeletes(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);
    REQUIRE_FALSE(composer.nonPreedit());

    // Typing carries on, just the old way — the client keeps whatever debris the
    // failed repair left, but nothing further is written behind its back.
    CHECK(composer.onKey(ascii('t')).effect == Effect::Preedit);
}
