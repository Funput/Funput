// English-mode gõ tắt: the composer gate plus applySettings pushing the switch.
// Matching rules live in the engine; this file only proves the Linux host reaches
// them on both typing paths and stays a passthrough when any switch is off.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

Settings englishOn(bool nonPreedit) {
    Settings settings;
    settings.method = Method::Telex;
    settings.enabled = false;
    settings.nonPreedit = nonPreedit;
    settings.shortcuts = {{"vn", "Việt Nam"}, {"sdt", "0901234567"}};
    return settings;
}

std::string typed(bool nonPreedit, const std::string &keys) {
    Composer composer(englishOn(nonPreedit));
    return typeDocument(composer, keys);
}

} // namespace

TEST_CASE("English mode expands on both typing paths") {
    CHECK(typed(true, "vn ") == "Việt Nam ");
    CHECK(typed(false, "vn ") == "Việt Nam ");
    CHECK(typed(true, "sdt ") == "0901234567 ");
    CHECK(typed(false, "hello ") == "hello ");
}

TEST_CASE("English mode expands on punctuation and follows smart case") {
    CHECK(typed(true, "vn.") == "Việt Nam.");
    CHECK(typed(true, "VN ") == "VIỆT NAM ");
    CHECK(typed(false, "Vn ") == "Việt Nam ");
}

TEST_CASE("Backspace can still correct a trigger in English mode") {
    Composer preedit(englishOn(false));
    type(preedit, "vnx");
    preedit.onKey(bare(keysym::BackSpace));
    CHECK(preedit.onKey(ascii(' ')).text == "Việt Nam ");

    Composer direct(englishOn(true));
    std::string document;
    for (char c : std::string("vnx")) applyPlan(document, direct.onKey(ascii(c)), c);
    direct.onKey(bare(keysym::BackSpace));
    popChars(document, 1);
    applyPlan(document, direct.onKey(ascii(' ')), ' ');
    CHECK(document == "Việt Nam ");
}

TEST_CASE("turning either switch off leaves English mode a passthrough") {
    Settings settings = englishOn(true);
    settings.shortcutsInEnglish = false;
    Composer offEnglish(settings);
    CHECK(typeDocument(offEnglish, "vn ") == "vn ");

    settings = englishOn(true);
    settings.shortcutsEnabled = false;
    Composer offTable(settings);
    CHECK(typeDocument(offTable, "vn ") == "vn ");
}

TEST_CASE("applySettings pushes shortcutsInEnglish to the engine") {
    Settings settings = englishOn(true);
    Composer composer(settings);
    CHECK(typeDocument(composer, "vn ") == "Việt Nam ");
    composer.settings().shortcutsInEnglish = false;
    composer.applySettings();
    CHECK(typeDocument(composer, "vn ") == "vn ");
}

TEST_CASE("the flip hotkey stays inert in English with a live table") {
    Settings settings = englishOn(false);
    settings.flipHotkey = FlipHotkey::CtrlShiftZ;
    Composer composer(settings);
    CHECK(composer.onKey(ctrlShift('z')).isNoop());
}
