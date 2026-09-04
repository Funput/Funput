// Reading settings.json: every wire name, the defaults a key's absence must leave in
// place, and what a file the reader cannot make sense of does *not* do.

#include <doctest/doctest.h>

#include "settings/settings.h"
#include "settings/support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("reload parses every field from its wire name") {
    writeSettingsFile(R"({
        "method": "telex_advanced",
        "toneStyle": "modern",
        "enabled": false,
        "smartRestore": false,
        "eagerRestore": false,
        "spellCheck": true,
        "autoCapitalize": true,
        "nonPreedit": true,
        "toggleHotkey": "ctrl_space",
        "flipHotkey": "ctrl_shift_x",
        "shortcutsEnabled": false,
        "shortcutSmartCase": false,
        "shortcuts": [{"trigger": "vn", "expansion": "Việt Nam"}]
    })");

    Settings settings;
    CHECK(settings.reload());
    CHECK(settings.method == Method::TelexAdvanced);
    CHECK(settings.toneStyle == ToneStyle::Modern);
    CHECK_FALSE(settings.enabled);
    CHECK_FALSE(settings.smartRestore);
    CHECK_FALSE(settings.eagerRestore);
    CHECK(settings.spellCheck);
    CHECK(settings.autoCapitalize);
    CHECK(settings.nonPreedit);
    CHECK(settings.toggleHotkey == Hotkey::CtrlSpace);
    CHECK(settings.flipHotkey == FlipHotkey::CtrlShiftX);
    CHECK_FALSE(settings.shortcutsEnabled);
    CHECK_FALSE(settings.shortcutSmartCase);
    REQUIRE(settings.shortcuts.size() == 1);
    CHECK(settings.shortcuts[0].first == "vn");
    CHECK(settings.shortcuts[0].second == "Việt Nam");
}

// The compatibility guarantee behind both gõ tắt switches: a settings.json written
// before they existed describes a table that expands, smart-cased. An update that
// read those absences as "off" would silently change what the table expands to.
TEST_CASE("the gõ tắt switches default to on when the file predates them") {
    writeSettingsFile(R"({
        "method": "telex",
        "shortcuts": [{"trigger": "vn", "expansion": "Việt Nam"}]
    })");

    Settings settings;
    REQUIRE(settings.reload());
    CHECK(settings.shortcutsEnabled);
    CHECK(settings.shortcutSmartCase);
}

TEST_CASE("reload reports whether anything actually changed") {
    writeSettingsFile(R"({"method": "vni", "enabled": true})");
    Settings settings;
    settings.method = Method::Telex;
    CHECK(settings.reload()); // method moved

    CHECK_FALSE(settings.reload()); // same file, same values
}

TEST_CASE("a corrupt or missing file leaves the current values alone") {
    writeSettingsFile("{ this is not json");
    Settings settings;
    settings.method = Method::TelexAdvanced;
    CHECK_FALSE(settings.reload());
    CHECK(settings.method == Method::TelexAdvanced);

    writeSettingsFile("[1, 2, 3]"); // valid JSON, wrong shape
    CHECK_FALSE(settings.reload());
    CHECK(settings.method == Method::TelexAdvanced);
}

TEST_CASE("unknown wire values fall back to the current setting") {
    writeSettingsFile(R"({"method": "dvorak", "toggleHotkey": "ctrl_alt_del"})");
    Settings settings;
    settings.method = Method::Vni;
    settings.toggleHotkey = Hotkey::CtrlBacktick;
    settings.reload();
    CHECK(settings.method == Method::Vni);
    CHECK(settings.toggleHotkey == Hotkey::CtrlBacktick);
}

TEST_CASE("toggleHotkey parses the extra presets") {
    writeSettingsFile(R"({"toggleHotkey": "super_space"})");
    Settings settings;
    REQUIRE(settings.reload());
    CHECK(settings.toggleHotkey == Hotkey::SuperSpace);
    writeSettingsFile(R"({"toggleHotkey": "ctrl_shift_space"})");
    CHECK(settings.reload());
    CHECK(settings.toggleHotkey == Hotkey::CtrlShiftSpace);
    writeSettingsFile(R"({"toggleHotkey": "alt_shift"})");
    CHECK(settings.reload());
    CHECK(settings.toggleHotkey == Hotkey::AltShift);
}
