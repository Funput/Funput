// Enter and Tab: keys that end the word being typed, yet must still reach the app,
// since Enter may send a message and Tab may move focus or accept a completion.
//
// With its switch on ("Bung gõ tắt khi bấm Enter" / "… Tab", both off by default), a
// gõ tắt trigger is expanded first and the key then goes on as itself: `ct` Enter
// writes `cơm tấm` and still sends. Same rule as `ShellState::trigger_key` on Windows.
// Only gõ tắt runs here, and no Backspace undo is armed (see
// `funput_engine_expand_shortcut`): the key that follows may have sent the text away.

#include "compose/composer/composer.h"

#include <string>
#include <utility>

namespace funput {

ComposePlan Composer::onTriggerKey(const KeyEvent &ev) {
    const bool enter = isEnter(ev);
    const bool wanted = enter ? settings_.shortcutOnEnter : settings_.shortcutOnTab;
    std::string expansion;
    const FunputResult result =
        wanted ? handle_.expandShortcut(expansion) : FunputResult{};
    // Enter leaves the caret on a new line, where the next word opens a sentence.
    if (enter && settings_.autoCapitalize) handle_.armCapitalization();
    if (result.action != ACTION_SEND) return commitBuffer(false);

    // The engine has committed the word; what is left is putting the expansion where
    // the word stood. Either way the key then moves the caret somewhere the journal
    // cannot follow, so it is forgotten, as `commitBuffer` does for any such key.
    if (!nonPreedit_.on) {
        // The word is still the preedit, not in the document: committing the
        // expansion replaces it, so `result.backspace` has nothing to delete.
        nonPreedit_.forget();
        return ComposePlan::flushThrough(std::move(expansion));
    }
    // Typed straight: the word is in the document, and the expansion is one more
    // repair of it, with the same guard against deleting into a selection.
    ComposePlan plan = planFromResult(result, 0, std::move(expansion));
    nonPreedit_.forget();
    plan.consumed = false;
    return plan;
}

} // namespace funput
