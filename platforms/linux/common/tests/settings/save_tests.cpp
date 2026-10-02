// Writing settings.json, and the mtime shortcut that decides whether to read it.

#include <doctest/doctest.h>

#include <fstream>
#include <iterator>
#include <string>

#include "settings/settings.h"
#include "settings/support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("reloadIfChanged skips a file whose mtime has not moved") {
    // "telex" differs from the Method::Vni default, so the first read reports a
    // change — the return value is "did any value move", not "did I read the file".
    writeSettingsFile(R"({"method": "telex"})");
    Settings settings;
    REQUIRE(settings.reloadIfChanged());
    CHECK_FALSE(settings.reloadIfChanged());
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

// The gõ tắt switches are options the addon reads, so — unlike the table itself —
// they ride along on a save. A VI/EN toggle must not drop the user's choice.
TEST_CASE("save keeps the gõ tắt switches") {
    writeSettingsFile(R"({"method": "telex"})");

    Settings settings;
    REQUIRE(settings.reload());
    settings.shortcutsEnabled = false;
    settings.shortcutSmartCase = false;
    settings.shortcutsInEnglish = false;
    settings.save();

    Settings reloaded;
    REQUIRE(reloaded.reload());
    CHECK_FALSE(reloaded.shortcutsEnabled);
    CHECK_FALSE(reloaded.shortcutSmartCase);
    CHECK_FALSE(reloaded.shortcutsInEnglish);
}

// The addon never edits the letters, so a VI/EN toggle's save must leave the key
// to the merge: writing its own copy back could undo a newer choice.
TEST_CASE("save never writes extraOnsets back over the file's") {
    writeSettingsFile(R"({"method": "telex", "extraOnsets": "z"})");
    Settings settings;
    REQUIRE(settings.reload());
    // The Settings app saves new letters before this process has reloaded.
    writeSettingsFile(R"({"method": "telex", "extraOnsets": "fz"})");
    settings.enabled = false;
    settings.save();

    Settings reloaded;
    REQUIRE(reloaded.reload());
    CHECK(reloaded.extraOnsets.id() == "zf");
    CHECK_FALSE(reloaded.enabled);
}

// A file without the key, or with anything but a string there, reads as none —
// the pre-feature spelling — instead of failing the rest of the file.
TEST_CASE("extraOnsets reads as none when absent or not a string") {
    writeSettingsFile(R"({"method": "vni", "extraOnsets": 7})");
    Settings settings;
    REQUIRE(settings.reload());
    CHECK(settings.method == Method::Vni);
    CHECK(settings.extraOnsets.empty());
    writeSettingsFile(R"({"method": "telex"})");
    REQUIRE(settings.reload());
    CHECK(settings.extraOnsets.empty());
}
