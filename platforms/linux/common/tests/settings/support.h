// Shared helper for the settings-bridge tests. Every case writes the file it then
// reads, so the tests do not depend on each other's leftovers (the composer's VI/EN
// toggle persists into the same sandbox file). The sandbox itself is
// XDG_CONFIG_HOME, set by main.cpp — the user's real settings.json is never touched.

#ifndef FUNPUT_TESTS_SETTINGS_SUPPORT_H
#define FUNPUT_TESTS_SETTINGS_SUPPORT_H

#include <doctest/doctest.h>

#include <fstream>
#include <iterator>
#include <string>

#include "settings/settings.h"

namespace funput::test {

inline void writeSettingsFile(const std::string &json) {
    std::ofstream out(Settings::path(), std::ios::trunc);
    REQUIRE(out.good());
    out << json;
}

// The file as `save()` left it, for the cases about which keys a save writes.
inline std::string readSettingsFile() {
    std::ifstream in(Settings::path());
    return {std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
}

} // namespace funput::test

#endif // FUNPUT_TESTS_SETTINGS_SUPPORT_H
