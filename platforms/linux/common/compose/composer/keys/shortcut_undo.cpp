// Backspace right after a gõ tắt expansion takes it back: `vn␣` became `Việt Nam␣`,
// and ⌫ writes `vn␣` again, which the engine then keeps as typed once. The rules of
// the undo — what it restores, the refusal it leaves behind, what spends it — are
// the engine's; this file decides only whether the document can be trusted with it.

#include "compose/composer/composer.h"

#include <string>
#include <utility>

#include "compose/composer/nonpreedit/document/match.h"

namespace funput {

std::optional<ComposePlan> Composer::undoShortcut() {
    // Nothing armed is the common case, and it costs one FFI call and nothing else.
    std::string written;
    if (!handle_.shortcutUndoWritten(written)) return std::nullopt;
    // Only typed straight into the document. A preedit client is one a document
    // repair cannot be trusted to reach (nonpreedit.h), and the undo is one.
    //
    // There, only with the same evidence `backspaceOutsideWord` asks of its own
    // delete: every journaled edit confirmed, no selection for the key to delete
    // instead, and the expansion still standing right before the caret. Anything
    // since — a click without a reset, an edit the app made — fails that last test.
    // Unsure is a plain Backspace, never a guess: deleting the wrong characters would
    // take text the user typed.
    const bool trusted = nonPreedit_.on && nonPreedit_.inSync && !nonPreedit_.selectionLive &&
                         endsWith(nonPreedit_.document.predicted(), written);
    if (!trusted) {
        handle_.dismissShortcutUndo();
        return std::nullopt;
    }
    std::string restored;
    const FunputResult result = handle_.undoShortcut(restored);
    if (result.action != ACTION_SEND) return std::nullopt;
    nonPreedit_.noteRepair(result.backspace, restored);
    // Not a deletion of a character: the shell must not re-open the word before the
    // caret, which would give the trigger back to the engine to expand again.
    nonPreedit_.backspaceJournaled = false;
    return ComposePlan::replace(result.backspace, std::move(restored));
}

} // namespace funput
