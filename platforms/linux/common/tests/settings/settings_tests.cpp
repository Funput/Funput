// The settings bridge. Every case writes the file it then reads, so the tests do
// not depend on each other's leftovers (the composer's VI/EN toggle persists into
// the same sandbox file).

#include <doctest/doctest.h>

#include <cstdlib>
#include <fstream>
#include <string>

#include "settings/settings.h"

using namespace funput;

namespace {

void writeSettingsFile(const std::string &json) {
    std::ofstream out(Settings::path(), std::ios::trunc);
    REQUIRE(out.good());
    out << json;
}

} // namespace

TEST_CASE("path follows XDG_CONFIG_HOME") {
    const std::string path = Settings::path();
    CHECK(path.rfind("/Funput/settings.json") != std::string::npos);
    CHECK(path.rfind(std::getenv("XDG_CONFIG_HOME"), 0) == 0);
}

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
    REQUIRE(settings.shortcuts.size() == 1);
    CHECK(settings.shortcuts[0].first == "vn");
    CHECK(settings.shortcuts[0].second == "Việt Nam");
}

TEST_CASE("reload reports whether anything actually changed") {
    writeSettingsFile(R"({"method": "vni", "enabled": true})");
    Settings settings;
    settings.method = Method::Telex;
    CHECK(settings.reload()); // method moved

    CHECK_FALSE(settings.reload()); // same file, same values
}

TEST_CASE("reloadIfChanged skips a file whose mtime has not moved") {
    // "telex" differs from the Method::Vni default, so the first read reports a
    // change — the return value is "did any value move", not "did I read the file".
    writeSettingsFile(R"({"method": "telex"})");
    Settings settings;
    REQUIRE(settings.reloadIfChanged());
    CHECK_FALSE(settings.reloadIfChanged());
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

TEST_CASE("save round-trips and preserves keys it does not own") {
    // shortcuts belong to the Settings UI: the addon reads them but must never
    // write them back, or a toggle would wipe the user's list. Leftover keys
    // (including a retired excludedApps list) must survive a merge save too.
    writeSettingsFile(R"({
        "method": "vni",
        "excludedApps": [{"id": "firefox"}],
        "shortcuts": [{"trigger": "vn", "expansion": "Việt Nam"}],
        "someFutureKey": 42
    })");

    Settings settings;
    REQUIRE(settings.reload());
    settings.method = Method::Telex;
    settings.enabled = false;
    settings.save();

    Settings reloaded;
    REQUIRE(reloaded.reload());
    CHECK(reloaded.method == Method::Telex);
    CHECK_FALSE(reloaded.enabled);
    REQUIRE(reloaded.shortcuts.size() == 1);

    std::ifstream in(Settings::path());
    const std::string raw((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
    CHECK(raw.find("someFutureKey") != std::string::npos);
    CHECK(raw.find("excludedApps") != std::string::npos);
}
