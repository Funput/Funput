#include "funput_engine.h"

#include <fstream>
#include <sstream>

#include <fcitx-utils/keysym.h>
#include <nlohmann/json.hpp>

bool FunputEngine::matchesToggle(const fcitx::Key &key) const {
    const auto states = key.states();
    switch (settings_.toggleHotkey) {
    case funput::Hotkey::CtrlBacktick:
        return states.test(fcitx::KeyState::Ctrl) && key.sym() == FcitxKey_grave;
    case funput::Hotkey::CtrlSpace:
        return states.test(fcitx::KeyState::Ctrl) && key.sym() == FcitxKey_space;
    case funput::Hotkey::AltShift: return false;
    }
    return false;
}

bool FunputEngine::matchesFlip(const fcitx::Key &key) const {
    const auto states = key.states();
    if (!states.test(fcitx::KeyState::Ctrl) || !states.test(fcitx::KeyState::Shift)) return false;
    switch (settings_.flipHotkey) {
    case funput::FlipHotkey::CtrlShiftZ:
        return key.sym() == FcitxKey_z || key.sym() == FcitxKey_Z;
    case funput::FlipHotkey::CtrlShiftX:
        return key.sym() == FcitxKey_x || key.sym() == FcitxKey_X;
    case funput::FlipHotkey::Off: return false;
    }
    return false;
}

void FunputEngine::toggleEnabled(fcitx::InputContext *context) {
    commitBuffer(context);
    effectiveEnabled_ = !effectiveEnabled_;
    settings_.enabled = effectiveEnabled_;
    handle_.setEnabled(effectiveEnabled_);
    settings_.save();
}

void FunputEngine::noteRecentApp(const std::string &program) {
    if (program.empty()) return;
    std::string path = funput::Settings::path();
    const std::string filename = "settings.json";
    const auto position = path.rfind(filename);
    if (position == std::string::npos) return;
    path.replace(position, filename.size(), "recent-apps.json");
    nlohmann::json apps = nlohmann::json::array();
    if (std::ifstream input(path); input) {
        nlohmann::json parsed = nlohmann::json::parse(input, nullptr, false);
        if (parsed.is_array()) apps = std::move(parsed);
    }
    for (const auto &app : apps) {
        if (app.is_object() && app.value("id", std::string()) == program) return;
    }
    apps.insert(apps.begin(), nlohmann::json{{"id", program}, {"name", program}});
    if (apps.size() > 12) apps.erase(apps.begin() + 12, apps.end());
    std::ofstream output(path, std::ios::trunc);
    if (output) output << apps.dump(2);
}
