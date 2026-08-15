#define DOCTEST_CONFIG_IMPLEMENT
#include <doctest/doctest.h>

#include <cstdlib>
#include <filesystem>

int main(int argc, char **argv) {
    // Every Settings read and write resolves through XDG_CONFIG_HOME, and the
    // composer persists a VI/EN toggle — so point the whole binary at a throwaway
    // directory instead of the developer's real ~/.config/Funput/settings.json.
    // `Settings::save()` writes but does not create its directory, so make it here.
    const std::filesystem::path sandbox =
        std::filesystem::temp_directory_path() / "funput-linux-tests";
    std::error_code ec;
    std::filesystem::remove_all(sandbox, ec);
    std::filesystem::create_directories(sandbox / "Funput", ec);
    ::setenv("XDG_CONFIG_HOME", sandbox.c_str(), 1);

    return doctest::Context(argc, argv).run();
}
