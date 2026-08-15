// Non-preedit mode: build the word in the document instead of in a preedit.
//
// A preedit can be lost. Some clients drop it rather than commit it when focus
// moves away, and the half-typed word goes with it (see "Known gaps" in
// platforms/linux/README.md). Committing each keystroke as it is typed and
// repairing the previous one removes the thing that can be lost.
//
// This is not a new idea in this codebase — it is what the Windows shell already
// does. `crates/funput-desktop/src/inject.rs` turns an engine result into "delete N
// characters, then type this", and the engine hands out that N itself as
// `FunputResult::backspace`. So this file does not diff strings against the
// document: it forwards the same numbers, and Linux, Windows and macOS stay one
// behaviour rather than three.
//
// # What the mode assumes of its shell
//
// Measured on GNOME/Wayland with the surrounding-text probe (see the Fcitx5
// shell's README section), because the answers are not obvious:
//
//   - `deleteSurroundingText` counts **characters**, not bytes — verified with `ế`,
//     one character and three bytes. `deleteChars` is therefore a character count
//     all the way down, with no conversion anywhere.
//   - Writes must be **serialized**. A commit/delete/commit burst issued without
//     waiting for the client to confirm the previous write destroyed text the user
//     had typed (`;;;` came back as `;;y`) in every one of nine attempts. A plan is
//     only safe to perform once the previous one is known to have landed; the shell
//     owns that, and nothing here may assume otherwise.
//   - Capability flags and `program()` are unreliable there, so neither this file
//     nor its callers may gate on them.
//
// The composer does not know whether the shell can honour any of that, which is why
// the mode is off unless something turns it on.

#include "compose/composer/composer.h"

namespace funput {

ComposePlan Composer::planFromResult(const FunputResult &result) {
    // ACTION_NONE means the engine produced nothing to show: the key was not part of
    // a composition, so the app types it itself. Mirrors `plan_inject`'s None arm.
    if (result.action == ACTION_NONE) return ComposePlan::passThrough();
    return ComposePlan::replace(result.backspace, Handle::output(result));
}

ComposePlan Composer::endComposition(bool consumed) {
    // Nothing to commit. The word reached the document one keystroke at a time, so
    // committing it here would type the whole word a second time.
    handle_.clear();
    return consumed ? ComposePlan::swallow() : ComposePlan::passThrough();
}

} // namespace funput
