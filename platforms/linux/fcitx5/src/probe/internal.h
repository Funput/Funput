// Shared between the probe's two halves: log.cpp owns the file and the snapshot,
// record.cpp owns what each event means. See probe.h.

#ifndef FUNPUT_PROBE_INTERNAL_H
#define FUNPUT_PROBE_INTERNAL_H

#include <chrono>
#include <string>

#include <fcitx/inputcontext.h>
#include <nlohmann/json.hpp>

namespace funput::probe::detail {

using Clock = std::chrono::steady_clock;

// Append one JSON object, stamped with a monotonic millisecond clock. No-op when no
// writable log path exists.
void write(nlohmann::json record);

// The surrounding text as the client currently reports it, or `valid: false`.
nlohmann::json snapshot(fcitx::InputContext *ic);

// The UTF-8 text in front of the caret, or empty when the client reports nothing
// usable. Fcitx5 gives the cursor in *characters* while the text is UTF-8, so this
// is the one place that conversion lives — comparing raw strings without it reports
// a false mismatch in any field that has content after the caret.
std::string textBeforeCursor(fcitx::InputContext *ic);

// What we last committed, so a surrounding-text update can be diffed against it.
// Typing is serial, so one slot is enough — no per-context bookkeeping.
struct LastCommit {
    std::string text;
    Clock::time_point at;
    bool pending = false;
};
LastCommit &lastCommit();

} // namespace funput::probe::detail

#endif // FUNPUT_PROBE_INTERNAL_H
