// The keystroke decision tree — the one copy of it.
//
// Until this file existed the same tree lived twice, in Fcitx5's `keyEvent()` and
// IBus's `processKeyEvent()`, matching branch for branch. The order of the tests
// below is that shared order, and it is load-bearing: see `classify()` for why the
// hotkeys and Backspace are matched ahead of the character tests. Ending a word —
// a boundary character, and every exit that commits the buffer — is in
// keys/boundary.cpp.

#include "compose/composer/composer.h"

namespace funput {

ComposePlan Composer::onKey(const KeyEvent &ev) {
    const KeyKind kind = classify(ev, settings_);
    // The toggle works in either mode — it is how the user gets back out of English.
    if (kind == KeyKind::Toggle) return toggleEnabled();
    if (!effectiveEnabled_) {
        if (!englishShortcutsActive()) return ComposePlan::passThrough();
        // No Vietnamese form to flip; swallowing the chord would steal Redo.
        if (kind == KeyKind::Flip) return ComposePlan::passThrough();
    }

    // A highlight the user made over text before the caret — a double-click on the word
    // just typed, a drag across it. The next key replaces or deletes it, so the word the
    // engine still holds is gone, and keeping it handed the next word its letters
    // (`énvieejt` where `việt` was meant). A Sheets suffix lies after the caret and
    // leaves the word intact, hence `selectionAfter == 0`.
    const bool highlighted =
        nonPreedit_.on && nonPreedit_.selectionLive && nonPreedit_.selectionAfter == 0;
    if (highlighted && (kind == KeyKind::Compose || kind == KeyKind::Boundary ||
                        kind == KeyKind::NumpadDigit)) {
        discard();
    }

    switch (kind) {
    case KeyKind::Toggle: // handled above
        break;

    case KeyKind::Modifier:
        // Ctrl and Shift go down before the Z of Ctrl+Shift+Z. Committing here would
        // end the word the hotkey is about to act on, so the press just goes through.
        return ComposePlan::passThrough();

    case KeyKind::Flip: {
        // The hotkey is always swallowed, but a flip that changed nothing (no word
        // composing) leaves the client alone.
        const FunputResult result = handle_.flipComposing();
        if (result.action == ACTION_NONE) return ComposePlan::swallow();
        if (nonPreedit_.on) return planFromResult(result, 0);
        return ComposePlan::preedit(handle_.buffer());
    }

    case KeyKind::Shortcut:
        // Ctrl+A, Alt+Tab, Super+…: commit what is composing, then let the app have
        // the key — swallowing it would break the shortcut.
        return commitBuffer(false);

    case KeyKind::Backspace:
        // Over a selection the key deletes that, not a character, so nothing shortens:
        // a highlight over the word takes the word with it, a suffix leaves it whole.
        if (nonPreedit_.on && nonPreedit_.selectionLive) {
            if (highlighted) discard();
            return ComposePlan::passThrough();
        }
        // The composing word shortens and the key is swallowed. With nothing
        // composing the key passes through, so the app deletes its own character.
        if (handle_.buffer().empty()) return backspaceOutsideWord();
        handle_.backspace();
        // Non-preedit: the character stands in the document, so the key passes
        // through and the app deletes its own — the engine is only kept in step, and
        // the deletion journaled. Same division of labour as `ShellState::on_backspace`
        // on Windows.
        if (nonPreedit_.on) return appPerforms(1, 0);
        return ComposePlan::preedit(handle_.buffer());

    case KeyKind::NonText:
        // Arrows, F-keys, Enter: the caret is about to move somewhere the engine
        // cannot follow, so commit first and pass the key through.
        if (settings_.autoCapitalize && ev.keysym == keysym::Return) {
            handle_.armCapitalization();
        }
        return commitBuffer(false);

    case KeyKind::NumpadDigit:
        // A keypad digit is a literal number, never a VNI tone/shape modifier.
        return onBoundary(ev.ch, KeySource::Numpad);

    case KeyKind::Boundary:
        return onBoundary(ev.ch, KeySource::Standard);

    case KeyKind::Compose: {
        const FunputResult result = handle_.process(static_cast<uint32_t>(ev.ch));
        if (nonPreedit_.on) return planFromResult(result, ev.ch);
        // The engine can swallow a character without composing anything (a lone
        // modifier key in VNI, say); with nothing to show, the key passes through.
        if (handle_.buffer().empty()) return ComposePlan::passThrough();
        return ComposePlan::preedit(handle_.buffer());
    }
    }
    return ComposePlan::passThrough();
}

} // namespace funput
