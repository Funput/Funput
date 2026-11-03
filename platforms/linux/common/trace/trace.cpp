// The trace's switches and its bound. What goes into a line: events.cpp; the file
// itself: sink.cpp.

#include "trace/trace.h"

#include <cstdlib>
#include <iterator>

#include <nlohmann/json.hpp>

namespace funput {

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
    : ringLines_(ringLines), maxBytes_(maxBytes), key_(randomSipKey()),
      epoch_(std::chrono::steady_clock::now()), sink_(std::move(path), maxBytes) {}

void Trace::readEnvironment() {
    const char *value = std::getenv("FUNPUT_TRACE");
    if (value != nullptr && *value != '\0' && std::string_view(value) != "0") {
        envOn_ = true;
        envRaw_ = std::string_view(value) == "raw";
    }
    // The file has not been read yet: what the environment says is all there is.
    configure(Settings{});
}

void Trace::configure(const Settings &settings) {
    // `traceRaw` alone is enough: asking for the text is asking for the trace.
    const bool on = !sink_.path().empty() && (settings.trace || settings.traceRaw || envOn_);
    const bool raw = on && (settings.traceRaw || envRaw_);
    if (on == enabled_ && raw == raw_) return;
    enabled_ = on;
    raw_ = raw;
    // The ring was filled under the old switches. A rewrite after this must not put
    // raw lines into a summary-mode file — the user may have scrubbed the file in
    // between, which is the point of switching raw off — nor lines of a trace that
    // was off into one that is on again.
    ring_.clear();
    if (!on) {
        sink_.close();
        return;
    }
    // Also when only `raw` moved: a reader has to know from which line on the
    // summaries became text. Not before the shell has named itself (start()), or
    // an engine enabled from the environment would open with two `start` lines.
    if (!shell_.empty()) announce();
}

void Trace::emit(nlohmann::ordered_json line) {
    if (!enabled_) return;
    // Nothing here may reach a key handler: the shells call this between reading the
    // document and performing the plan. A trace that cannot be written is switched off.
    try {
        // `replace`, not the default `strict`: surrounding text is whatever the client
        // sent, and an invalid byte in it must not cost the line.
        constexpr auto replace = nlohmann::json::error_handler_t::replace;
        std::string dumped = line.dump(-1, ' ', false, replace);
        if (dumped.size() + 1 > maxBytes_) {
            // A raw reading of a very long document can exceed the whole bound. Kept
            // as is, the rewrite below would leave the file empty; what stays is a
            // note that says what went missing, so the file is bounded and still
            // ends on the newest line.
            nlohmann::ordered_json dropped = this->line("note");
            dropped["what"] = "dropped " + line["ev"].get<std::string>() + " line of " +
                              std::to_string(dumped.size()) + " bytes";
            dumped = dropped.dump(-1, ' ', false, replace);
        }
        ring_.push_back(std::move(dumped));
        if (ring_.size() > ringLines_) ring_.pop_front();
        if (!sink_.isOpen() && !sink_.open()) {
            fail();
            return;
        }
        const std::string &latest = ring_.back();
        if (!(sink_.fits(latest.size()) ? sink_.append(latest) : rewrite())) fail();
    } catch (...) {
        fail();
    }
}

bool Trace::rewrite() {
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
    return sink_.rewrite(ring_);
}

void Trace::fail() {
    enabled_ = false;
    raw_ = false;
    ring_.clear();
    sink_.close();
}

} // namespace funput
