// Backspace over committed text in non-preedit: which channel deletes, and when the
// word the caret lands on may be re-opened. Both hinge on whether the document the
// composer reads is current, which is what these cases pin.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sync.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

// `gõ` + `x` restores `gox`; Space, then ⌫⌫ deletes the space and the `x`, and `x`
// again must put the tone back. The first Backspace was taken over but not recorded,
// so the second had no proof the document was current and went to the app — and the
// repair after an app-handled Backspace is the one Chrome drops. `go` + `õ`: `goõ`.
TEST_CASE("consecutive Backspaces all stay on Funput's channel") {
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    std::string document = typeInSync(composer, "goxx ");
    REQUIRE(document == "gox ");

    const ComposePlan first = composer.onKey(bare(keysym::BackSpace));
    CHECK(first.consumed);
    popChars(document, first.deleteChars);
    CHECK_FALSE(composer.adoptWordBeforeBackspace("gox ")); // `gox` is not a syllable
    composer.observeDocument(document);
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
