// The trace's switches and its file. What goes into a line: events.cpp.

#include "trace/trace.h"

#include <cstdlib>
#include <filesystem>
#include <iterator>
#include <random>
#include <system_error>

#include <nlohmann/json.hpp>

namespace funput {

namespace {

uint64_t randomSalt() {
    std::random_device device;
    return (static_cast<uint64_t>(device()) << 32) ^ device();
}

} // namespace

Trace &Trace::shared() {
    static Trace instance(defaultPath());
    static const bool environmentRead = (instance.readEnvironment(), true);
    (void)environmentRead;
    return instance;
}

std::string Trace::defaultPath() {
    std::string base;
    if (const char *xdg = std::getenv("XDG_CACHE_HOME"); xdg && *xdg) {
        base = xdg;
    } else if (const char *home = std::getenv("HOME"); home && *home) {
        base = std::string(home) + "/.cache";
    } else {
        return {};
    }
    return base + "/funput/trace.jsonl";
}

Trace::Trace(std::string path, size_t ringLines, size_t maxBytes)
    : path_(std::move(path)), ringLines_(ringLines), maxBytes_(maxBytes), salt_(randomSalt()),
      epoch_(std::chrono::steady_clock::now()) {}

void Trace::readEnvironment() {
    const char *value = std::getenv("FUNPUT_TRACE");
    if (value == nullptr || *value == '\0' || std::string_view(value) == "0") return;
    envOn_ = true;
    envRaw_ = std::string_view(value) == "raw";
}

void Trace::configure(const Settings &settings) {
    // `traceRaw` alone is enough: asking for the text is asking for the trace.
    const bool on = !path_.empty() && (settings.trace || settings.traceRaw || envOn_);
    const bool raw = on && (settings.traceRaw || envRaw_);
    if (on == enabled_ && raw == raw_) return;
    enabled_ = on;
    raw_ = raw;
    if (!on) {
        out_.close();
        return;
    }
    // Also when only `raw` moved: a reader has to know from which line on the
    // summaries became text.
    announce();
}

void Trace::emit(nlohmann::ordered_json line) {
    if (!enabled_) return;
    // Nothing here may reach a key handler: the shells call this between reading the
    // document and performing the plan. A trace that cannot be written is switched off.
    try {
        // `replace`, not the default `strict`: surrounding text is whatever the client
        // sent, and an invalid byte in it must not cost the line.
        ring_.push_back(line.dump(-1, ' ', false, nlohmann::json::error_handler_t::replace));
        if (ring_.size() > ringLines_) ring_.pop_front();
        if (!out_.is_open() && !open()) return;
        const std::string &latest = ring_.back();
        if (bytes_ + latest.size() + 1 > maxBytes_) {
            rewrite();
        } else {
            out_ << latest << '\n' << std::flush;
            bytes_ += latest.size() + 1;
        }
        if (!out_) fail();
    } catch (...) {
        fail();
    }
}

bool Trace::open() {
    std::error_code ec;
    const std::filesystem::path file(path_);
    std::filesystem::create_directories(file.parent_path(), ec);
    if (ec) {
        fail();
        return false;
    }
    // Append: a trace spans engine restarts, and the bound below keeps it finite.
    const auto size = std::filesystem::file_size(file, ec);
    bytes_ = ec ? 0 : static_cast<size_t>(size);
    out_.open(path_, std::ios::app);
    if (!out_) {
        fail();
        return false;
    }
    // Raw mode writes what the user types. Whatever the umask said, nobody else reads it.
    namespace fs = std::filesystem;
    fs::permissions(file, fs::perms::owner_read | fs::perms::owner_write, ec);
    return true;
}

void Trace::rewrite() {
    // The file holds the newest lines that fit. The ring is bounded by lines and the
    // file by bytes, and a raw trace of a long document can make the ring the larger
    // of the two — so the ring is cut to what the file takes, from the oldest end.
    size_t total = 0;
    auto first = ring_.end();
    while (first != ring_.begin() && total + std::prev(first)->size() + 1 <= maxBytes_) {
        --first;
        total += first->size() + 1;
    }
    ring_.erase(ring_.begin(), first);
    out_.close();
    out_.open(path_, std::ios::trunc);
    for (const std::string &line : ring_) out_ << line << '\n';
    out_.flush();
    bytes_ = total;
}

void Trace::fail() {
    enabled_ = false;
    raw_ = false;
    out_.close();
    out_.clear();
}

} // namespace funput
