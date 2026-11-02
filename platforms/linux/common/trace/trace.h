// The diagnostic trace both Linux shells write, in one format.
//
// The symptoms in issue #640 — a word that cannot be re-opened, a repair that lands
// one character off — depend on *when* the client answers, which no test on a
// developer's machine reproduces and no `g_debug` reaches (ibus-daemon points the
// engine's stdio at /dev/null). So the shells write what they saw to
// $XDG_CACHE_HOME/funput/trace.jsonl, one JSON object per line, and a bug report can
// carry the file. `t` is milliseconds from a monotonic clock since the trace started;
// the `start` line carries the wall clock once.
//
// Off by default, and private when on: a string is written as its character count
// and a salted hash (trace/summary.h) unless the user opts into raw text separately.
// The switches are hidden keys in settings.json, picked up live by the settings
// watcher, because the engines are spawned by a daemon that no shell environment
// variable reaches; `FUNPUT_TRACE=1|raw` is honoured for a hand-started engine and
// can only add to what the file says.
//
// Bounded: an in-memory ring of the latest lines, and once the file passes
// `maxBytes` it is truncated and rewritten from the ring, so it always holds the end
// of the session and never grows past the bound. Every line is flushed, since the
// session worth tracing may be the one that crashes. I/O never throws out of a key
// handler: a failure switches the trace off and typing carries on.
//
// The composer knows nothing of this. The shells trace what they hand it and what
// it hands back (`Observation`, `ComposePlan`), so tracing on or off cannot change
// a plan.

#ifndef FUNPUT_TRACE_TRACE_H
#define FUNPUT_TRACE_TRACE_H

#include <chrono>
#include <cstddef>
#include <cstdint>
#include <deque>
#include <fstream>
#include <string>
#include <string_view>

#include <nlohmann/json_fwd.hpp>

#include "compose/composer/nonpreedit/verdict.h"
#include "compose/key/classify.h"
#include "compose/plan.h"
#include "settings/settings.h"

namespace funput {

class Trace {
public:
    // The process-wide trace at `defaultPath()`, the one the shells use. Also the only
    // instance that reads FUNPUT_TRACE, so a test's explicit path is never switched on
    // by the developer's environment.
    static Trace &shared();
    // $XDG_CACHE_HOME/funput/trace.jsonl, else ~/.cache/…; empty when neither is set,
    // and a trace with no path can never be enabled.
    static std::string defaultPath();

    explicit Trace(std::string path, size_t ringLines = 2000, size_t maxBytes = 1 << 20);
    Trace(const Trace &) = delete;
    Trace &operator=(const Trace &) = delete;

    // Apply `trace` / `traceRaw`; call again on every settings reload. Turning the
    // trace on writes a `start` line.
    void configure(const Settings &settings);
    // Callers check this before gathering anything for an event, so a key handled
    // with the trace off costs one branch.
    bool enabled() const { return enabled_; }
    bool raw() const { return raw_; }

    // --- events (events.cpp). Each is a no-op while disabled. ------------------

    // Names the shell; written as the `start` line now if enabled, and again each
    // time the trace is switched on. Repeating the same name writes nothing.
    void start(std::string_view shell);
    // A client took focus, under the name the shell has for it (may be empty).
    void focus(std::string_view client);
    // One key as classified. A printable key's identity is content: only its kind and
    // modifiers are written unless raw.
    void key(KeyKind kind, const KeyEvent &ev);
    // The document read before a key, with what the composer made of it.
    void reading(const std::string &before, bool answered, bool selection,
                 uint32_t selectedAfter, const Observation &seen);
    void plan(const ComposePlan &plan);
    // Direct typing switched on or off, and why. Only when the value changed.
    void mode(bool on, std::string_view reason);
    // Whether a Backspace re-opened the word before it.
    void adopt(bool taken);
    void note(std::string_view what);

private:
    void readEnvironment();
    // A line with `t` and `ev` filled in, for an event to complete.
    nlohmann::ordered_json line(std::string_view event) const;
    // A string as the trace is allowed to describe it (summary.h), raw if opted in.
    nlohmann::ordered_json text(const std::string &utf8) const;
    void announce();
    void emit(nlohmann::ordered_json line);
    bool open();
    void rewrite();
    void fail();

    std::string path_;
    size_t ringLines_;
    size_t maxBytes_;
    bool enabled_ = false;
    bool raw_ = false;
    // From FUNPUT_TRACE, read once by shared(). Adds to the settings, never subtracts.
    bool envOn_ = false;
    bool envRaw_ = false;
    // Never written anywhere. See summary.h.
    uint64_t salt_;
    std::chrono::steady_clock::time_point epoch_;
    std::string shell_;
    std::deque<std::string> ring_;
    std::ofstream out_;
    size_t bytes_ = 0;
};

} // namespace funput

#endif // FUNPUT_TRACE_TRACE_H
