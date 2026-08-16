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

// A client that drops every delete.
void applyIgnoringDeletes(std::string &document, const ComposePlan &plan, char key) {
    if (plan.effect != Effect::Replace) {
        applyPlan(document, plan, key);
        return;
    }
    document += plan.text;
}

// Type until the mode or re-toning is refused, feeding the client each plan. Returns
// the document as that client left it.
std::string typeUntilRefused(Composer &composer, const std::string &keys,
                             void (*client)(std::string &, const ComposePlan &, char)) {
    std::string document;
    for (char c : keys) {
        composer.observeDocument(document);
        if (!composer.nonPreedit()) break;
        client(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);
    return document;
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

TEST_CASE("a refusal sticks until the next context") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    typeUntilRefused(composer, "tieengs", applyIgnoringDeletes);
    REQUIRE_FALSE(composer.nonPreedit());

    // What IBus does on every keystroke. Before the latch this revived the mode one
    // key after it had been stood down, so the verdict was worth nothing there.
    composer.setNonPreedit(true);
    CHECK_FALSE(composer.nonPreedit());

    // A new client is a new question, and the only thing that reopens it.
    composer.onFocusChanged();
    composer.setNonPreedit(true);
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
