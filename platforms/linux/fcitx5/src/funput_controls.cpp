// Bookkeeping that is neither typing nor framework plumbing: the recent-apps list
// the Settings window offers when the user picks apps to exclude.

#include "funput_engine.h"

#include <fstream>

#include <nlohmann/json.hpp>

#include "settings/settings.h"

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
