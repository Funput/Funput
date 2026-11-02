// Shared helpers for the trace tests: a file of their own inside the XDG_CACHE_HOME
// sandbox main.cpp set up, a Settings that switches the trace on, and the file read
// back as parsed lines. Every case builds its own `Trace` on an explicit path — never
// `Trace::shared()`, whose path and FUNPUT_TRACE belong to the developer's machine.

#ifndef FUNPUT_TESTS_TRACE_SANDBOX_H
#define FUNPUT_TESTS_TRACE_SANDBOX_H

#include <doctest/doctest.h>

#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <iterator>
#include <string>
#include <vector>

#include <nlohmann/json.hpp>

#include "settings/settings.h"
#include "trace/trace.h"

namespace funput::test {

// A fresh path under the sandbox; whatever an earlier case left there is removed.
inline std::string tracePath(const std::string &name) {
    const char *cache = std::getenv("XDG_CACHE_HOME");
    REQUIRE(cache != nullptr);
    const std::filesystem::path file = std::filesystem::path(cache) / "trace-tests" / name;
    std::error_code ec;
    std::filesystem::remove(file, ec);
    return file.string();
}

inline Settings tracing(bool raw = false) {
    Settings settings;
    settings.trace = true;
    settings.traceRaw = raw;
    return settings;
}

inline std::string readFile(const std::string &path) {
    std::ifstream in(path);
    return {std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
}

// Every line parsed; a line that is not one JSON object fails the case.
inline std::vector<nlohmann::json> readLines(const std::string &path) {
    std::vector<nlohmann::json> lines;
    std::ifstream in(path);
    for (std::string line; std::getline(in, line);) {
        nlohmann::json parsed = nlohmann::json::parse(line, nullptr, false);
        REQUIRE(parsed.is_object());
        lines.push_back(std::move(parsed));
    }
    return lines;
}

// The line with this `ev`, counting from the end: the one the case just caused.
inline nlohmann::json lastEvent(const std::vector<nlohmann::json> &lines, const std::string &ev) {
    for (auto it = lines.rbegin(); it != lines.rend(); ++it) {
        if ((*it)["ev"] == ev) return *it;
    }
    FAIL("no `" << ev << "` line in the trace");
    return {};
}

} // namespace funput::test

#endif // FUNPUT_TESTS_TRACE_SANDBOX_H
