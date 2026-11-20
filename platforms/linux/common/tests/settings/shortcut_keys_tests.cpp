// "Bung gõ tắt khi bấm Enter / Tab": two switches the Settings app owns. The addon
// reads them, off unless the file says otherwise, and never writes them back.

#include <doctest/doctest.h>

#include "settings/settings.h"
#include "settings/support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("the Enter and Tab switches read their wire names, off when absent") {
    writeSettingsFile(R"({"shortcutOnEnter": true, "shortcutOnTab": true})");
    Settings settings;
    CHECK(settings.reload());
    CHECK(settings.shortcutOnEnter);
    CHECK(settings.shortcutOnTab);

    // A file written before the switches existed, or by a hand that typed `1`: off.
    writeSettingsFile(R"({"shortcutOnTab": 1})");
    CHECK(settings.reload()); // the change is reported, so the composer reads it
    CHECK_FALSE(settings.shortcutOnEnter);
    CHECK_FALSE(settings.shortcutOnTab);
}

TEST_CASE("the Enter and Tab switches never interrupt a word") {
    Settings other;
    other.shortcutOnEnter = true;
    other.shortcutOnTab = true;
    CHECK(Settings{}.sameTyping(other));
}

TEST_CASE("save leaves the Enter and Tab switches as the file has them") {
    writeSettingsFile(R"({"shortcutOnEnter": true})");
    Settings settings;
    settings.reload();
    settings.shortcutOnTab = true; // a copy the addon never writes
    settings.enabled = false;      // what a VI/EN toggle saves
    settings.save();

    const std::string saved = readSettingsFile();
    CHECK(saved.find("\"shortcutOnEnter\": true") != std::string::npos);
    CHECK(saved.find("shortcutOnTab") == std::string::npos);
}
