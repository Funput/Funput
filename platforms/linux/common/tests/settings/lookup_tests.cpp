// Where settings.json lives (settings/lookup.cpp).

#include <doctest/doctest.h>

#include <cstdlib>
#include <string>

#include "settings/settings.h"

using namespace funput;

TEST_CASE("path follows XDG_CONFIG_HOME") {
    const std::string path = Settings::path();
    CHECK(path.rfind("/Funput/settings.json") != std::string::npos);
    CHECK(path.rfind(std::getenv("XDG_CONFIG_HOME"), 0) == 0);
}
