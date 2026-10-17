// A settings push that only touches gõ tắt must not interrupt the word being typed.
//
// The Settings app saves settings.json on every keystroke in a gõ tắt field, and in
// non-preedit mode every keystroke reaches that field. The shells reload on each save
// and call `applySettings()`, which used to drop the composing word — so the next key
// found an empty engine and was typed raw. See compose/composer/state/applied.h.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

Composer directTelex() {
    Settings settings;
    settings.method = Method::Telex;
    settings.nonPreedit = true;
    return Composer(settings);
}

// Type `keys` the way a shell would, extending `document` as it goes.
void feed(Composer &composer, std::string &document, const std::string &keys) {
    for (char c : keys) applyPlan(document, composer.onKey(ascii(c)), c);
}

} // namespace

TEST_CASE("a gõ tắt table change mid-word leaves the word alone") {
    Composer composer = directTelex();
    std::string document;
    feed(composer, document, "ba");
    REQUIRE(composer.isComposing());

    composer.settings().shortcuts = {{"vn", "Việt Nam"}};
    composer.applySettings();

    CHECK(composer.isComposing());
    feed(composer, document, "s");
    CHECK(document == "bá");
}

TEST_CASE("one push per keystroke, as the Settings app causes, never breaks the word") {
    Composer composer = directTelex();
    std::string document;
    std::string trigger;
    for (char c : std::string("bas")) {
        feed(composer, document, std::string(1, c));
        // Every key the user types into a gõ tắt field rewrites the table.
        trigger += c;
        composer.settings().shortcuts = {{trigger, "x"}};
        composer.applySettings();
    }
    CHECK(document == "bá");
}

TEST_CASE("a gõ tắt switch change mid-word leaves the word alone") {
    Composer composer = directTelex();
    std::string document;
    feed(composer, document, "ba");

    composer.settings().shortcutsEnabled = false;
    composer.applySettings();

    CHECK(composer.isComposing());
    feed(composer, document, "s");
    CHECK(document == "bá");
}

TEST_CASE("the new gõ tắt table is in effect after a push that spared the word") {
    Settings settings;
    settings.method = Method::Telex;
    settings.nonPreedit = false; // the expansion is asserted as one preedit commit
    Composer composer(settings);
    REQUIRE(typeDocument(composer, "vn ") == "vn ");

    composer.settings().shortcuts = {{"vn", "Việt Nam"}};
    composer.applySettings();

    CHECK(typeDocument(composer, "vn ") == "Việt Nam ");
}

TEST_CASE("a change to how words compose still drops the word") {
    Composer composer = directTelex();
    std::string document;
    feed(composer, document, "ba");
    REQUIRE(composer.isComposing());

    composer.settings().method = Method::Vni;
    composer.applySettings();

    CHECK_FALSE(composer.isComposing());
    // The digit is a tone only if the engine still held `ba`.
    feed(composer, document, "1");
    CHECK(document == "ba1");
}

TEST_CASE("switching non-preedit off still drops the word") {
    Composer composer = directTelex();
    std::string document;
    feed(composer, document, "ba");
    REQUIRE(composer.isComposing());

    composer.settings().nonPreedit = false;
    composer.applySettings();

    CHECK_FALSE(composer.isComposing());
    CHECK_FALSE(composer.nonPreedit());
}

TEST_CASE("a per-client non-preedit veto survives a gõ tắt push") {
    Composer composer = directTelex();
    composer.setNonPreedit(false); // the shell's call for a client that cannot take it

    composer.settings().shortcuts = {{"vn", "Việt Nam"}};
    composer.applySettings();

    CHECK_FALSE(composer.nonPreedit());
}
