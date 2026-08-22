#include "settings/settings.h"

#include <cstdlib>

namespace funput {

std::string Settings::path() {
    const char *xdg = std::getenv("XDG_CONFIG_HOME");
    std::string base;
    if (xdg && *xdg) {
        base = xdg;
    } else if (const char *home = std::getenv("HOME"); home && *home) {
        base = std::string(home) + "/.config";
    } else {
        return {};
    }
    return base + "/Funput/settings.json";
}

} // namespace funput
