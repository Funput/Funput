// A selected suffix after the caret is spreadsheet autocomplete, not a user
// highlight. Non-preedit must clear it and still apply the tone, or `chư` plus
// Sheets' `ứ` becomes `chưứ`.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/selection.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

Composer typing(Method method) {
    Composer composer = composerFor(method);
    composer.setNonPreedit(true);
    return composer;
}

void typePrefix(Composer &composer, std::string &document, const std::string &keys) {
    for (char c : keys) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
}

// Sheets has filled the matching suffix and selected it. The composer only sees
// text before the caret; the document the shell repairs includes both.
ComposePlan toneWithSuffix(Composer &composer, std::string &document, char key) {
    composer.observeDocument(document, true, true, 1);
    document += "ứ";
    const ComposePlan plan = composer.onKey(ascii(key));
    applyPlan(document, plan, key);
    return plan;
}

} // namespace

TEST_CASE("selectedAfterCaret is a suffix, not select-all") {
    CHECK(selectedAfterCaret(3, 4) == 1);
    CHECK(selectedAfterCaret(0, 4) == 0);
    CHECK(selectedAfterCaret(4, 3) == 0);
    CHECK(selectedAfterCaret(3, 3) == 0);
}

TEST_CASE("a Sheets suffix is cleared so VNI can still place the tone") {
    Composer composer = typing(Method::Vni);
    std::string document;
    typePrefix(composer, document, "chu7");
    REQUIRE(document == "chư");
    const ComposePlan plan = toneWithSuffix(composer, document, '1');
    CHECK(plan.effect == Effect::Replace);
    CHECK(plan.deleteAfterChars == 1);
    CHECK(plan.deleteChars == 1);
    CHECK(plan.text == "ứ");
    CHECK(document == "chứ");
    CHECK(composer.nonPreedit());
    CHECK(composer.isComposing());
}

TEST_CASE("a Sheets suffix is cleared so Telex can still place the tone") {
    Composer composer = typing(Method::Telex);
    std::string document;
    typePrefix(composer, document, "chuw");
    REQUIRE(document == "chư");
    CHECK(toneWithSuffix(composer, document, 's').text == "ứ");
    CHECK(document == "chứ");
    CHECK(composer.nonPreedit());
    CHECK(composer.isComposing());
}

TEST_CASE("a user highlight is not treated as a suffix") {
    Composer composer = typing(Method::Telex);
    std::string document;
    typePrefix(composer, document, "chuw");
    composer.observeDocument(document, true, true, 0);
    // Not a tone for the highlighted `chư`: the key replaces it and starts a word.
    CHECK(composer.onKey(ascii('s')).isNoop());
    CHECK(composer.nonPreedit());
}

// The user highlights the word just typed — a double-click, a drag — and the next key
// replaces or deletes it. The engine still holds the word, and must not hand it to
// whatever is typed next: that was `énvieejt` where `việt` was meant.
TEST_CASE("typing over a highlighted word starts a new word") {
    // Whether the first key happens to rewrite the old word decides if the old code
    // noticed, so try letters that do and letters that merely append.
    const std::pair<const char *, const char *> words[] = {
        {"vieejt", "việt"}, {"nam", "nam"}, {"xin", "xin"}, {"hoa", "hoa"}, {"as", "á"}};
    for (const auto &[keys, expected] : words) {
        CAPTURE(std::string(keys));
        Composer composer = typing(Method::Telex);
        std::string document;
        typePrefix(composer, document, "tieengs");
        REQUIRE(document == "tiếng");

        composer.observeDocument(document, true, true, 0);
        document.clear(); // what the app does with the key: replace the selection
        applyPlan(document, composer.onKey(ascii(keys[0])), keys[0]);
        typePrefix(composer, document, keys + 1);
        CHECK(document == expected);
    }
}

TEST_CASE("Backspace over a highlighted word leaves nothing for the next word to inherit") {
    Composer composer = typing(Method::Telex);
    std::string document;
    typePrefix(composer, document, "tieengs");

    composer.observeDocument(document, true, true, 0);
    CHECK(composer.onKey(bare(keysym::BackSpace)).isNoop()); // the app deletes the selection
    CHECK_FALSE(composer.isComposing());
    document.clear();
    typePrefix(composer, document, "vieejt");
    CHECK(document == "việt");
}

TEST_CASE("Backspace over a Sheets suffix deletes the suffix, not a letter of the word") {
    Composer composer = typing(Method::Vni);
    std::string document;
    typePrefix(composer, document, "chu7");

    composer.observeDocument(document, true, true, 1);
    CHECK(composer.onKey(bare(keysym::BackSpace)).isNoop());
    // The typed `chư` is intact, so the word is too: the tone still lands on it.
    CHECK(composer.isComposing());
}
