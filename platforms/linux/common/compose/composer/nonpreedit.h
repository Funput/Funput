// Non-preedit mode: build the word in the document instead of in a preedit.
//
// A preedit can be lost. Some clients drop it rather than commit it when focus moves
// away, and the half-typed word goes with it (see "Known gaps" in
// platforms/linux/README.md). Committing each keystroke as it is typed and repairing
// the previous one removes the thing that can be lost.
//
// Not a new idea in this codebase — it is what the Windows shell already does.
// `crates/funput-desktop/src/inject.rs` turns an engine result into "delete N
// characters, then type this", and the engine hands out that N itself as
// `FunputResult::backspace`. So nothing here diffs strings against the document: it
// forwards the same numbers, and Linux, Windows and macOS stay one behaviour.
//
// Writes are deliberately *not* serialized — waiting for each to be confirmed would
// cost ~25ms a keystroke and stall on the 61% of commits a client never answers, and
// real typing cannot produce the burst that made waiting look necessary. The README
// carries the measurements. But not waiting before a write is not the same as never
// checking after one, and that is what this file is for.
//
// # Judging a client
//
// The mode depends on `deleteSurroundingText` being honoured, and there is no way to
// know whether it will be until one is dropped. After a repair "delete N, write T"
// issued against document `D`, the next reading can only be one of three strings:
//
//   `D` less N, then `T`   it worked
//   `D` then `T`           the delete was dropped
//   `D`                    the client has not answered yet
//
// Only the middle is a verdict. Treating "not what I expected" as failure would stand
// the mode down on most keystrokes, curing one broken client by breaking the feature
// for everyone. A repair that deletes nothing makes the first two identical, so that
// case is excluded rather than left to luck.
//
// Which leaves one client this can never judge: the one whose document reads empty
// every time. All three strings collapse into one there, so no repair is ever worth a
// verdict. `BlindWrites` in nonpreedit/verdict.h is the answer to that one.
//
// The verdict belongs to the client it was about, not to the focus it was reached in:
// a shell may re-activate the same client many times over — Fcitx5 does so on every
// capability change — and each of those must not reopen a settled question. See
// nonpreedit/clients.h.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_H
#define FUNPUT_COMPOSE_NONPREEDIT_H

#include <cstdint>
#include <string>
#include <string_view>

#include "compose/composer/nonpreedit/clients.h"
#include "compose/composer/nonpreedit/verdict.h"
#include "ffi/utf8.h"

namespace funput {

struct NonPreeditState {
    // The mode as it applies right now. `refused` outranks it: once a client has been
    // caught dropping a delete, a shell re-asserting the mode must not undo that. IBus
    // re-decides on every keystroke, so without the latch a verdict lasted one key.
    bool on = false;
    bool refused = false;
    // Which clients that latch has already been spent on — see clients.h. Kept across
    // `reset()`, since a client is the same client whether it was re-focused or merely
    // re-activated by a capability change.
    ClientMemory clients;
    // How often this client has answered with nothing while being written into.
    BlindWrites blind;
    bool retoneAllowed = true;
    // Whether the client answered since the last keystroke. Only the shell can see it,
    // and `BlindWrites` needs it to tell an empty answer from no answer at all.
    bool answered = false;
    // Whether the client is holding a selection. Only the shell can see this, and the
    // Backspace path below needs it: taking a Backspace over while text is selected
    // would swallow the key the user pressed to delete that selection.
    bool selectionLive = false;
    // Whether the last repair was seen to land. Distinct from "no verdict", which also
    // covers a client that has said nothing — see `observe()`. Only a confirmation
    // proves the document we are holding is current, and only then is it safe to take
    // a Backspace over instead of letting the app perform it.
    bool inSync = false;

    // The document as last seen and the repair last written into it — enough to say
    // what it should read now, and what it would read had the delete been dropped.
    std::string lastDoc;
    std::string repairText;
    uint32_t repairDeleted = 0;
    bool repairAfterAdopt = false;
    // Armed by a re-opened word, spent by the repair that follows it. That repair is
    // the one a client like the address bar drops.
    bool justAdopted = false;

    // A client took focus: whatever the last one did says nothing about this one —
    // unless it *is* this one, in which case a verdict already reached still stands.
    // `on` survives either way; that is the shell's decision, not something learned
    // here.
    void reset(std::string_view client) {
        refused = clients.focus(client);
        blind.reset();
        answered = false;
        retoneAllowed = true;
        selectionLive = false;
        inSync = false;
        lastDoc.clear();
        repairText.clear();
        repairDeleted = 0;
        repairAfterAdopt = false;
        justAdopted = false;
    }

    // Record a repair just emitted, so the next reading can be judged against it.
    void noteRepair(uint32_t deleted, const std::string &text) {
        repairDeleted = deleted;
        repairText = text;
        repairAfterAdopt = justAdopted;
        justAdopted = false;
    }

    // Judge `document` against that record and forget it either way — one reading is
    // all a repair gets. Also remembers `document` as the latest.
    Verdict observe(const std::string &document) {
        Verdict verdict = Verdict::Unknown;
        if (!repairText.empty()) {
            const std::string dropped = lastDoc + repairText;
            const std::string applied = dropLast(lastDoc, repairDeleted) + repairText;
            inSync = document == applied;
            if (document == dropped && dropped != applied) {
                verdict = repairAfterAdopt ? Verdict::RefuseRetone : Verdict::RefuseMode;
            }
            // The comparison above is blind to a document that is always empty, which
            // is a failure of its own rather than an absence of one.
            if (verdict == Verdict::Unknown && blind.observe(document, answered)) {
                verdict = Verdict::RefuseMode;
            }
            repairText.clear();
            repairDeleted = 0;
            repairAfterAdopt = false;
        } else {
            // Nothing was written, so nothing was confirmed. A document that moved on
            // its own — the user clicking, or selecting with the mouse — lands here.
            inSync = false;
        }
        lastDoc = document;
        return verdict;
    }
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_H
