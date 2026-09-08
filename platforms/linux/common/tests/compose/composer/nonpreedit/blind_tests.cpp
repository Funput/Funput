// The client whose document never shows anything.
//
// Non-preedit judges a repair by comparing three strings — the document with the delete
// applied, the same document with the delete dropped, and the document untouched. With
// nothing in front of the caret all three are equal, so no repair can ever be judged
// and the mode writes on blind. Cursor's editor is exactly that client: it answers
// surrounding text on every keystroke, which arms the mode, and answers it empty every
// time, while dropping every delete.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/refusal.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

// The client sent surrounding text since the last keystroke. Only an answer can
// accuse — see "silence is never a verdict" below.
constexpr bool kAnswered = true;

TEST_CASE("a client that never shows its document loses the mode") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    const std::string blind;
    for (char c : std::string("tieengs")) {
        composer.observeDocument(blind, false, kAnswered);
        if (!composer.nonPreedit()) break;
        composer.onKey(ascii(c));
    }
    composer.observeDocument(blind, false, kAnswered);

    CHECK_FALSE(composer.nonPreedit());
    // The composition goes with the mode: the engine believed it owned a word the
    // document does not have.
    CHECK_FALSE(composer.isComposing());
}

TEST_CASE("standing down inside the first word") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // The point of the rule is that it costs one word, not one session. Four characters
    // is inside `tieengs`, so the verdict lands before a second word is ever typed.
    const std::string blind;
    int keys = 0;
    for (char c : std::string("tieengs")) {
        composer.observeDocument(blind, false, kAnswered);
        if (!composer.nonPreedit()) break;
        composer.onKey(ascii(c));
        ++keys;
    }
    CHECK(keys <= 5);
}

TEST_CASE("silence is never a verdict") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // 39% of commits go unanswered, and an unanswered write reads as an empty document
    // exactly like an answer of nothing. Counting those stood the mode down in Chrome —
    // the client this whole feature exists for — so only an actual answer may accuse.
    const std::string blind;
    for (char c : std::string("tieengs ")) {
        composer.observeDocument(blind, false, !kAnswered);
        REQUIRE(composer.nonPreedit());
        composer.onKey(ascii(c));
    }
    composer.observeDocument(blind, false, !kAnswered);

    CHECK(composer.nonPreedit());
}

TEST_CASE("a document that answers is never called blind") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);

    // An empty field answers "" as well — but only until its first commit lands. Any
    // reading with something in it starts the count over, so a client that is merely
    // slow, or a caret that returns to the start of a line, cannot accumulate its way
    // to a verdict.
    std::string document;
    for (char c : std::string("tieengs tieengs ")) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);

    CHECK(composer.nonPreedit());
    CHECK(document == "tiếng tiếng ");
}

TEST_CASE("a blind verdict is remembered against the client") {
    Composer composer = composerFor(Method::Telex);
    composer.onFocusChanged("cursor");
    composer.setNonPreedit(true);

    const std::string blind;
    for (char c : std::string("tieengs")) {
        composer.observeDocument(blind, false, kAnswered);
        if (!composer.nonPreedit()) break;
        composer.onKey(ascii(c));
    }
    REQUIRE_FALSE(composer.nonPreedit());

    // Same client re-activating — Fcitx5 does this on every capability change — must
    // not be handed the mode back and cost another word.
    composer.onFocusChanged("cursor");
    composer.setNonPreedit(true);
    CHECK_FALSE(composer.nonPreedit());
}
