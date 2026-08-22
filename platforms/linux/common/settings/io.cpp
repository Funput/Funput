#include "settings/settings.h"

#include <fstream>
#include <sys/stat.h>

#include <nlohmann/json.hpp>

namespace funput {
namespace {
using json = nlohmann::json;

Method parseMethod(const std::string &value) {
    if (value == "telex") return Method::Telex;
    if (value == "telex_advanced") return Method::TelexAdvanced;
    return Method::Vni;
}

const char *methodString(Method method) {
    switch (method) {
    case Method::Telex: return "telex";
    case Method::TelexAdvanced: return "telex_advanced";
    case Method::Vni: return "vni";
    }
    return "vni";
}

ToneStyle parseTone(const std::string &value) {
    return value == "modern" ? ToneStyle::Modern : ToneStyle::Traditional;
}

const char *toneString(ToneStyle tone) {
    return tone == ToneStyle::Modern ? "modern" : "traditional";
}

Hotkey parseHotkey(const std::string &value) {
    if (value == "ctrl_space") return Hotkey::CtrlSpace;
    if (value == "alt_shift") return Hotkey::AltShift;
    if (value == "super_space") return Hotkey::SuperSpace;
    if (value == "ctrl_shift_space") return Hotkey::CtrlShiftSpace;
    return Hotkey::CtrlBacktick;
}

const char *hotkeyString(Hotkey hotkey) {
    switch (hotkey) {
    case Hotkey::CtrlSpace: return "ctrl_space";
    case Hotkey::AltShift: return "alt_shift";
    case Hotkey::SuperSpace: return "super_space";
    case Hotkey::CtrlShiftSpace: return "ctrl_shift_space";
    case Hotkey::CtrlBacktick: return "ctrl_backtick";
    }
    return "ctrl_backtick";
}

FlipHotkey parseFlip(const std::string &value) {
    if (value == "ctrl_shift_z") return FlipHotkey::CtrlShiftZ;
    if (value == "ctrl_shift_x") return FlipHotkey::CtrlShiftX;
    return FlipHotkey::Off;
}

const char *flipString(FlipHotkey hotkey) {
    if (hotkey == FlipHotkey::CtrlShiftZ) return "ctrl_shift_z";
    if (hotkey == FlipHotkey::CtrlShiftX) return "ctrl_shift_x";
    return "off";
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
    toneStyle = parseTone(data.value("toneStyle", std::string(toneString(toneStyle))));
    enabled = data.value("enabled", enabled);
    smartRestore = data.value("smartRestore", smartRestore);
    eagerRestore = data.value("eagerRestore", eagerRestore);
    spellCheck = data.value("spellCheck", spellCheck);
    autoCapitalize = data.value("autoCapitalize", autoCapitalize);
    nonPreedit = data.value("nonPreedit", nonPreedit);
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
           autoCapitalize != previous.autoCapitalize || nonPreedit != previous.nonPreedit ||
           toggleHotkey != previous.toggleHotkey || flipHotkey != previous.flipHotkey ||
           shortcuts != previous.shortcuts;
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
    data["nonPreedit"] = nonPreedit;
    data["toggleHotkey"] = hotkeyString(toggleHotkey);
    data["flipHotkey"] = flipString(flipHotkey);
    std::ofstream output(file, std::ios::trunc);
    if (output) output << data.dump(2);
}

} // namespace funput
