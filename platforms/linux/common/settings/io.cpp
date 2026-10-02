#include "settings/settings.h"

#include <fstream>
#include <sys/stat.h>

#include <nlohmann/json.hpp>

#include "settings/io/names.h"

namespace funput {
namespace {
using json = nlohmann::json;
using namespace names;

// Absent, or anything but a string, is no letter: that is how a file written
// before the key existed spelled, and the Settings app always writes it.
ExtraOnsetLetters parseOnsets(const json &data) {
    const auto value = data.find("extraOnsets");
    if (value == data.end() || !value->is_string()) return {};
    return ExtraOnsetLetters::fromId(value->get<std::string>());
}
} // namespace

bool Settings::reloadIfChanged() {
    const std::string file = path();
    if (file.empty()) return false;
    struct stat status {};
    if (::stat(file.c_str(), &status) != 0) return false;
    if (static_cast<int64_t>(status.st_mtime) == lastMtime_) return false;
    return reload();
}

bool Settings::reload() {
    const std::string file = path();
    struct stat status {};
    if (file.empty() || ::stat(file.c_str(), &status) != 0) return false;
    lastMtime_ = static_cast<int64_t>(status.st_mtime);
    std::ifstream input(file);
    if (!input) return false;
    json data = json::parse(input, nullptr, false);
    if (!data.is_object()) return false;
    const Settings previous = *this;
    method = parseMethod(data.value("method", std::string(methodString(method))));
    // A file without this key predates tone-style settings. Keep those users on
    // the placement they already had; only a missing file receives the new default.
    toneStyle = parseTone(data.value("toneStyle", std::string("traditional")));
    enabled = data.value("enabled", enabled);
    smartRestore = data.value("smartRestore", smartRestore);
    eagerRestore = data.value("eagerRestore", eagerRestore);
    spellCheck = data.value("spellCheck", spellCheck);
    autoCapitalize = data.value("autoCapitalize", autoCapitalize);
    extraOnsets = parseOnsets(data);
    nonPreedit = data.value("nonPreedit", nonPreedit);
    shortcutsEnabled = data.value("shortcutsEnabled", shortcutsEnabled);
    shortcutSmartCase = data.value("shortcutSmartCase", shortcutSmartCase);
    shortcutsInEnglish = data.value("shortcutsInEnglish", shortcutsInEnglish);
    toggleHotkey = parseHotkey(data.value("toggleHotkey", std::string(hotkeyString(toggleHotkey))));
    flipHotkey = parseFlip(data.value("flipHotkey", std::string(flipString(flipHotkey))));
    shortcuts.clear();
    if (const auto items = data.find("shortcuts"); items != data.end() && items->is_array()) {
        for (const auto &item : *items) {
            if (item.is_object() && item.contains("trigger") && item["trigger"].is_string() &&
                item.contains("expansion") && item["expansion"].is_string()) {
                shortcuts.emplace_back(item["trigger"].get<std::string>(),
                                       item["expansion"].get<std::string>());
            }
        }
    }
    return method != previous.method || toneStyle != previous.toneStyle ||
           enabled != previous.enabled || smartRestore != previous.smartRestore ||
           eagerRestore != previous.eagerRestore || spellCheck != previous.spellCheck ||
           autoCapitalize != previous.autoCapitalize || extraOnsets != previous.extraOnsets ||
           nonPreedit != previous.nonPreedit || toggleHotkey != previous.toggleHotkey ||
           flipHotkey != previous.flipHotkey ||
           shortcuts != previous.shortcuts || shortcutsEnabled != previous.shortcutsEnabled ||
           shortcutSmartCase != previous.shortcutSmartCase ||
           shortcutsInEnglish != previous.shortcutsInEnglish;
}

void Settings::save() const {
    const std::string file = path();
    if (file.empty()) return;
    json data = json::object();
    if (std::ifstream input(file); input) {
        json previous = json::parse(input, nullptr, false);
        if (previous.is_object()) data = std::move(previous);
    }
    data["method"] = methodString(method);
    data["toneStyle"] = toneString(toneStyle);
    data["enabled"] = enabled;
    data["smartRestore"] = smartRestore;
    data["eagerRestore"] = eagerRestore;
    data["spellCheck"] = spellCheck;
    data["autoCapitalize"] = autoCapitalize;
    // Not `extraOnsets`: the addon never changes the letters, so writing its copy
    // back could only undo a choice the Settings app saved since the last reload,
    // or drop a letter a newer build wrote. The merge above keeps the key as is.
    data["nonPreedit"] = nonPreedit;
    data["shortcutsEnabled"] = shortcutsEnabled;
    data["shortcutSmartCase"] = shortcutSmartCase;
    data["shortcutsInEnglish"] = shortcutsInEnglish;
    data["toggleHotkey"] = hotkeyString(toggleHotkey);
    data["flipHotkey"] = flipString(flipHotkey);
    std::ofstream output(file, std::ios::trunc);
    if (output) output << data.dump(2);
}

} // namespace funput
