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

#ifndef FUNPUT_COMPOSE_NONPREEDIT_H
#define FUNPUT_COMPOSE_NONPREEDIT_H

#include <cstdint>
#include <string>

namespace funput {

// What the last repair turned out to be worth.
enum class Verdict : uint8_t {
    // Either it landed or the client has not said. Both mean carry on.
    Unknown,
    // The delete was dropped on a repair that followed a re-opened word. Chrome's
    // address bar does exactly this: ordinary repairs work, but one issued straight
    // after the app handled a Backspace itself is discarded. Only re-toning need go.
    RefuseRetone,
    // The delete was dropped on an ordinary repair, so nothing written here can be
    // trusted. The mode goes.
    RefuseMode,
};

struct NonPreeditState {
    // The mode as it applies right now. `refused` outranks it: once a client has been
    // caught dropping a delete, a shell re-asserting the mode must not undo that. IBus
    // re-decides on every keystroke, so without the latch a verdict lasted one key.
    bool on = false;
    bool refused = false;
    bool retoneAllowed = true;

    // The document as last seen and the repair last written into it — enough to say
    // what it should read now, and what it would read had the delete been dropped.
    std::string lastDoc;
    std::string repairText;
    uint32_t repairDeleted = 0;
    bool repairAfterAdopt = false;
    // Armed by a re-opened word, spent by the repair that follows it. That repair is
    // the one a client like the address bar drops.
    bool justAdopted = false;

    // A new input context: whatever the last client did says nothing about this one.
    void reset();

    // Record a repair just emitted, so the next reading can be judged against it.
    void noteRepair(uint32_t deleted, const std::string &text);

    // Judge `document` against that record and forget it either way — one reading is
    // all a repair gets. Also remembers `document` as the latest.
    Verdict observe(const std::string &document);
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_H
