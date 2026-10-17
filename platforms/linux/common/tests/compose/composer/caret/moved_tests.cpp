// A caret the user moved from under the word being composed.
//
// Qt, Chromium and Firefox reset an input method on a click only while they hold a
// preedit, and direct typing holds none, so nobody tells the composer. The engine keeps
// `tieng`, and the next tone key would delete characters in front of the new caret that
// Funput never wrote. The text *after* the caret is what gives it away: our own typing
// cannot change it, however late the client reports the text before it.

#include <doctest/doctest.h>

#include <string>

#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

Composer typing(Method method) {
    Composer composer = composerFor(method);
    composer.setNonPreedit(true);
    return composer;
}

// What a client shows around the caret.
struct Field {
    std::string before;
    std::string after;
};

void read(Composer &composer, const Field &field, bool answered = true) {
    composer.observeDocument(field.before, false, answered, 0, field.after);
}

// Type `keys` the way a shell drives the composer: read the field, then press the key.
void typeInto(Composer &composer, Field &field, const std::string &keys) {
    for (char c : keys) {
        read(composer, field);
        applyPlan(field.before, composer.onKey(ascii(c)), c);
    }
    read(composer, field);
}

} // namespace

TEST_CASE("a caret moved mid-word drops the word instead of repairing the wrong place") {
    Composer composer = typing(Method::Telex);
    Field field;
    typeInto(composer, field, "tieng");
    REQUIRE(composer.isComposing());

    // A click between `n` and `g`: same text, caret elsewhere, and no reset.
    field = {"tien", "g"};
    read(composer, field);

    CHECK_FALSE(composer.isComposing());
    // The tone key goes through as typed. Repairing `tieng` here would delete the `ien`
    // in front of the caret and write `iến` over text Funput never typed.
    CHECK(composer.onKey(ascii('s')).isNoop());
    // Nothing the client did was wrong, so the mode stays.
    CHECK(composer.nonPreedit());
}

TEST_CASE("an unknown text after the caret is never judged") {
    Composer composer = typing(Method::Telex);
    Field field;
    typeInto(composer, field, "tieng");

    // The same click, from a shell that has no text after the caret to offer.
    composer.observeDocument("tien", false, true, 0);
    CHECK(composer.isComposing());
}

TEST_CASE("typing is never a move, at the end of a line or in the middle of a text") {
    // The text after the caret stays put while the one before it grows and changes.
    for (const char *after : {"", " world"}) {
        Composer composer = typing(Method::Telex);
        Field field{"hello ", after};
        typeInto(composer, field, "tieengs");

        CHECK(composer.isComposing());
        CHECK(field.before == "hello tiếng");
    }
}

TEST_CASE("a client that answers late is not a client that moved the caret") {
    Composer composer = typing(Method::Telex);
    std::string document;
    std::string stale;
    for (char c : std::string("tieengs")) {
        // Always one key behind, as Chrome can be, with the text after the caret as it was.
        composer.observeDocument(stale, false, true, 0, std::string());
        stale = document;
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    CHECK(document == "tiếng");
    CHECK(composer.isComposing());
}

TEST_CASE("a client that has not answered cannot have moved the caret") {
    Composer composer = typing(Method::Telex);
    Field field;
    typeInto(composer, field, "tieng");

    read(composer, {"tien", "g"}, /*answered=*/false);
    CHECK(composer.isComposing());
}

TEST_CASE("a Sheets suffix is not a moved caret, nor is the reading after we clear it") {
    Composer composer = typing(Method::Vni);
    Field field;
    typeInto(composer, field, "chu7");
    REQUIRE(field.before == "chư");

    // The suffix is selected: text after the caret that comes and goes with every key.
    composer.observeDocument(field.before, true, true, 1, std::string("ứ"));
    field.before += "ứ";
    applyPlan(field.before, composer.onKey(ascii('1')), '1');
    REQUIRE(field.before == "chứ");

    // Our repair removed the suffix, so the text after the caret is empty again.
    read(composer, field);
    CHECK(composer.isComposing());
}

TEST_CASE("the first reading after a selection only sets the baseline") {
    Composer composer = typing(Method::Telex);
    Field field;
    typeInto(composer, field, "tieng");

    // A suffix is selected, then stays unselected: not a caret that went elsewhere.
    composer.observeDocument(field.before, true, true, 1, std::string("x"));
    read(composer, {field.before, "x"});
    CHECK(composer.isComposing());
}

TEST_CASE("a new focus forgets what was after the caret") {
    Composer composer = typing(Method::Telex);
    Field field{"hello ", " world"};
    typeInto(composer, field, "tieng");

    composer.onFocusChanged();
    read(composer, {"other", ""});

    CHECK(composer.isComposing());
}
