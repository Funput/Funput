// Where the probe writes, and how it reads the client's surrounding text.

#include "probe/internal.h"
#include "probe/probe.h"

#include <cstdlib>
#include <fstream>

#include "settings/settings.h"

namespace funput::probe {

namespace {

std::string logPath() {
    if (const char *override = std::getenv("FUNPUT_PROBE_LOG"); override && *override) {
        return override;
    }
    std::string path = Settings::path();
    const std::string filename = "settings.json";
    const auto position = path.rfind(filename);
    if (position == std::string::npos) return {};
    return path.replace(position, filename.size(), "probe.jsonl");
}

} // namespace

bool enabled() {
    static const bool on = [] {
        const char *value = std::getenv("FUNPUT_PROBE");
        return value != nullptr && *value == '1';
    }();
    return on;
}

namespace detail {

LastCommit &lastCommit() {
    static LastCommit value;
    return value;
}

void write(nlohmann::json record) {
    static const std::string path = logPath();
    if (path.empty()) return;
    record["ms"] =
        std::chrono::duration_cast<std::chrono::milliseconds>(Clock::now().time_since_epoch())
            .count();
    std::ofstream out(path, std::ios::app);
    if (out) out << record.dump() << '\n';
}

nlohmann::json snapshot(fcitx::InputContext *ic) {
    const auto &surrounding = ic->surroundingText();
    if (!surrounding.isValid()) return nlohmann::json{{"valid", false}};
    return nlohmann::json{
        {"valid", true},
        {"len", surrounding.text().size()},
        {"cursor", surrounding.cursor()},
        {"anchor", surrounding.anchor()},
        // anchor != cursor means a selection is live. Committing then deleting here
        // would eat the client's selected text — the browser-autofill hazard.
        {"selection", surrounding.cursor() != surrounding.anchor()},
    };
}

} // namespace detail

} // namespace funput::probe
