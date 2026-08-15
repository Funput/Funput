// Phase 0 of the non-preedit work: measure, do not implement.
//
// Non-preedit mode (commit as you type, repair with `deleteSurroundingText`) only
// works where the client reports surrounding text *correctly and in time*. Nobody
// knows which Linux apps actually do — the answers below decide whether the feature
// is viable, and what its fallback rule must be:
//
//   1. Which apps advertise CapabilityFlag::SurroundingText at all?
//   2. When they do, is the text real — does it match what we just committed?
//   3. How late does it arrive at typing speed? (If it lags, the composer must
//      never wait on it mid-word.)
//   4. Does InputContext::program() work under Wayland, where the text-input-v3
//      protocol carries no app id?
//   5. Is a selection ever active when we commit? That is the browser-autofill
//      hazard: a delete would eat the selected text instead of our own.
//
// Diagnostic only, and **off unless `FUNPUT_PROBE=1`** — every entry point below
// returns immediately otherwise, so a normal install pays nothing and behaves
// identically. Delete this directory once the questions are answered.
//
// Output: one JSON object per line, appended to `$FUNPUT_PROBE_LOG` (default
// ~/.config/Funput/probe.jsonl). See analyze.sh for the summary.

#ifndef FUNPUT_PROBE_H
#define FUNPUT_PROBE_H

#include <string>

#include <fcitx/inputcontext.h>

namespace funput::probe {

// Whether `FUNPUT_PROBE=1` was set when the addon loaded. Read once.
bool enabled();

// A new input context took focus: record who it is and what it claims to support.
void noteFocus(fcitx::InputContext *ic);

// We just committed `text`. Records the state *before* the client sees it, so the
// next surrounding-text update can be diffed against it.
void noteCommit(fcitx::InputContext *ic, const std::string &text);

// The client published new surrounding text. Records how long after our commit it
// arrived and whether it ends with what we committed.
void noteSurroundingUpdate(fcitx::InputContext *ic);

} // namespace funput::probe

#endif // FUNPUT_PROBE_H
