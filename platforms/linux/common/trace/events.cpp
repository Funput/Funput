// What each trace line says. The switches and the file: trace.cpp; the words for
// the composer's enums: events/names.h.

#include "trace/trace.h"

#include <cstdio>
#include <ctime>

#include <unistd.h>

#include <nlohmann/json.hpp>

#include "ffi/utf8.h"
#include "trace/events/names.h"
#include "trace/summary/text.h"

namespace funput {

namespace {

using json = nlohmann::ordered_json;
using namespace funput::trace;

std::string hex(uint64_t value, int width) {
    char buffer[24];
    std::snprintf(buffer, sizeof buffer, "%0*llx", width, static_cast<unsigned long long>(value));
    return buffer;
}

std::string wallClock() {
    const std::time_t now = std::time(nullptr);
    std::tm utc{};
    gmtime_r(&now, &utc);
    char buffer[32];
    std::strftime(buffer, sizeof buffer, "%Y-%m-%dT%H:%M:%SZ", &utc);
    return buffer;
}

} // namespace

json Trace::line(std::string_view event) const {
    const auto elapsed = std::chrono::steady_clock::now() - epoch_;
    return json{{"t", std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count()},
                {"ev", std::string(event)}};
}

json Trace::text(const std::string &utf8) const {
    const TextSummary summary = summarize(utf8, key_);
    json out{{"chars", summary.chars}};
    // Not for the empty string: its count says it all, and its hash would be the one
    // value a trace prints for a known input, on every line that carries it.
    if (summary.chars != 0) out["hash"] = hex(summary.hash, 16);
    if (raw_) out["text"] = utf8;
    return out;
}

void Trace::start(std::string_view shell) {
    // IBus builds one engine per input context, each of which introduces itself; the
    // process started once, so the line is written once.
    if (shell_ == shell) return;
    shell_ = std::string(shell);
    if (enabled_) announce();
}

void Trace::announce() {
    // `t` counts from the `start` line above it, so this one reads 0.
    epoch_ = std::chrono::steady_clock::now();
    json out = line("start");
    out["shell"] = shell_;
    out["wall"] = wallClock();
    out["pid"] = static_cast<int>(getpid());
    out["raw"] = raw_;
    emit(std::move(out));
}

void Trace::focus(std::string_view client) {
    if (!enabled_) return;
    json out = line("focus");
    out["client"] = std::string(client);
    emit(std::move(out));
}

void Trace::key(KeyKind kind, const KeyEvent &ev) {
    if (!enabled_) return;
    json out = line("key");
    out["kind"] = kindName(kind);
    // Which non-text key arrived is behaviour; a printable key's keysym is content,
    // and so are its modifiers — Shift on a letter is the letter's case.
    if (raw_ || !printsCharacter(kind)) {
        if (const std::string mods = modsName(ev.mods); !mods.empty()) out["mods"] = mods;
        out["keysym"] = hex(ev.keysym, 1);
    }
    if (raw_ && ev.ch != 0) {
        std::string ch;
        appendUtf8(ch, static_cast<uint32_t>(ev.ch));
        out["ch"] = ch;
    }
    emit(std::move(out));
}

void Trace::reading(const std::string &before, bool answered, bool selection,
                    uint32_t selectedAfter, const Observation &seen) {
    if (!enabled_) return;
    json out = line("reading");
    out["before"] = text(before);
    out["answered"] = answered;
    out["selection"] = selection;
    out["selectedAfter"] = selectedAfter;
    out["verdict"] = verdictName(seen.verdict);
    out["inSync"] = seen.inSync;
    emit(std::move(out));
}

void Trace::plan(const ComposePlan &plan) {
    if (!enabled_) return;
    json out = line("plan");
    out["effect"] = effectName(plan.effect);
    out["consumed"] = plan.consumed;
    if (plan.effect != Effect::None) out["text"] = text(plan.text);
    if (plan.effect == Effect::Replace) {
        out["deleteChars"] = plan.deleteChars;
        out["deleteAfterChars"] = plan.deleteAfterChars;
    }
    emit(std::move(out));
}

void Trace::mode(bool on, std::string_view reason) {
    if (!enabled_) return;
    json out = line("mode");
    out["on"] = on;
    out["reason"] = std::string(reason);
    emit(std::move(out));
}

void Trace::adopt(bool taken) {
    if (!enabled_) return;
    json out = line("adopt");
    out["taken"] = taken;
    emit(std::move(out));
}

void Trace::note(std::string_view what) {
    if (!enabled_) return;
    json out = line("note");
    out["what"] = std::string(what);
    emit(std::move(out));
}

} // namespace funput
