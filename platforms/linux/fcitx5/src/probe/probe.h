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
// Reading the client is only half of it. Non-preedit has to *write* — commit, then
// repair with `deleteSurroundingText` — and nothing here exercises that path until
// the self-test below runs. Question 6, added after the first round of data:
//
//   6. Does deleteSurroundingText do anything, does it count *characters* rather
//      than bytes, and does it stay ordered against the commits around it?
//
// The character question is the one that matters: "ế" is one character but three
// bytes, so an ASCII-only check passes on a client that counts bytes and then
// corrupts Vietnamese in the field.
//
// Output: one JSON object per line, appended to `$FUNPUT_PROBE_LOG` (default
// ~/.config/Funput/probe.jsonl). See ../../analyze-probe.sh for the summary.

#ifndef FUNPUT_PROBE_H
#define FUNPUT_PROBE_H

#include <string>

#include <fcitx/event.h>
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
// arrived and whether it ends with what we committed, and drives the self-test.
void noteSurroundingUpdate(fcitx::InputContext *ic);

// --- the write self-test (question 6) ---------------------------------------
//
// Never automatic: a broken write path leaves visible debris in the document, so the
// user has to ask for it with the caret in a scratch field. A working run writes and
// then removes exactly what it wrote, leaving no trace — and the debris of a failing
// run is itself the answer. Triggering again always restarts, which is how a run
// that stalled waiting for an update the client never sent returns to a known state.
//
// Triggered by **typing `;;;p`**, not by a chord. The first attempt used Ctrl+Alt+P
// and logged nothing at all — not even the "refused" branch — which means GNOME
// Shell on Wayland swallows the chord before it reaches the input method. Typed text
// cannot be intercepted that way: delivering it *is* what an input method is for, so
// the sequence works the same on X11, Wayland, GNOME and KDE alike.
//
// The leading `;;;` land in the document and stay there. Removing them would need
// `deleteSurroundingText` — the very thing under test — and they are useful anyway:
// a non-empty baseline also proves the delete does not run past what it wrote.
//
// Ctrl+Alt+P still works where it survives, and every Ctrl+Alt chord is logged, so
// the next run says definitively whether such chords arrive at all.

// Start the self-test if this key is the trigger. True when the key was consumed.
bool maybeStartSelfTest(fcitx::KeyEvent &event);

} // namespace funput::probe

#endif // FUNPUT_PROBE_H
