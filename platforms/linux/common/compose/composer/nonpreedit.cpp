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

#include <vector>

#include "compose/key/boundary.h"
#include "ffi/utf8.h"

namespace funput {

ComposePlan Composer::planFromResult(const FunputResult &result) {
    // ACTION_NONE means the engine produced nothing to show: the key was not part of
    // a composition, so the app types it itself. Mirrors `plan_inject`'s None arm.
    if (result.action == ACTION_NONE) return ComposePlan::passThrough();
    return ComposePlan::replace(result.backspace, Handle::output(result));
}

bool Composer::adoptWordBeforeBackspace(const std::string &textBeforeCaret) {
    if (!nonPreedit_ || !effectiveEnabled_) return false;
    std::vector<uint32_t> chars = decodeUtf8(textBeforeCaret);
    if (chars.empty()) return false;
    // The app has not deleted it yet, so drop it here to see where the caret lands.
    chars.pop_back();

    size_t start = chars.size();
    while (start > 0 && !isBoundary(static_cast<char32_t>(chars[start - 1]), settings_.method)) {
        --start;
    }
    // The caret landed on a separator, not on a word — nothing to re-open. Mirrors
    // `CommittedTail::backspace` on Windows, which is the same rule sourced from a
    // shadow copy because a hook shell has no document to read.
    if (start == chars.size()) return false;

    std::string word;
    for (size_t i = start; i < chars.size(); ++i) appendUtf8(word, chars[i]);
    // The engine refuses anything that is not a Vietnamese syllable, which is what
    // keeps English words and URLs literal.
    return handle_.adopt(word);
}

ComposePlan Composer::endComposition(bool consumed) {
    // Nothing to commit. The word reached the document one keystroke at a time, so
    // committing it here would type the whole word a second time.
    handle_.clear();
    return consumed ? ComposePlan::swallow() : ComposePlan::passThrough();
}

} // namespace funput
