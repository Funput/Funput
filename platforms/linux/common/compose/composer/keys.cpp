// The keystroke decision tree — the one copy of it.
//
// Until this file existed the same tree lived twice, in Fcitx5's `keyEvent()` and
// IBus's `processKeyEvent()`, matching branch for branch. The order of the tests
// below is that shared order, and it is load-bearing: see `classify()` for why the
// hotkeys and Backspace are matched ahead of the character tests.

#include "compose/composer/composer.h"

#include "ffi/utf8.h"

namespace funput {

ComposePlan Composer::commitBuffer(bool consumed) {
    // Non-preedit typed the word into the document as it went, so there is nothing
    // left to commit. This one early-out covers every exit that funnels through
    // here: flush(), toggleEnabled(), a shortcut and a caret key.
    if (nonPreedit_.on) return endComposition(consumed);
    std::string buffer = handle_.buffer();
    handle_.clear();
    // An empty buffer still yields Effect::Commit — with no text that means "drop
    // the preedit and commit nothing", which is what ends a cancelled composition.
    return ComposePlan{Effect::Commit, std::move(buffer), consumed};
}

ComposePlan Composer::onBoundary(char32_t scalar, KeySource source) {
    const std::string before = handle_.buffer();
    if (before.empty()) {
        // Nothing composing: the boundary is just a character. It still reaches the
        // engine when auto-capitalize is on, which tracks sentence ends.
        if (settings_.autoCapitalize) handle_.process(static_cast<uint32_t>(scalar), source);
        return ComposePlan::passThrough();
    }
    const FunputResult result = handle_.process(static_cast<uint32_t>(scalar), source);
    // The engine's output already ends with the boundary character, and the word in
    // front of it is already in the document — so the repair covers both at once,
    // with none of the splicing the preedit path needs below.
    if (nonPreedit_.on) return planFromResult(result);
    std::string word = before;
    if (result.action == ACTION_SEND) {
        word = Handle::output(result);
        // The engine's output ends with the boundary character it was fed; drop it
        // and re-append below, so both paths commit exactly one copy of it. Every
        // boundary is ASCII (see `isBoundary`), so one byte is one character here.
        if (!word.empty()) word.pop_back();
    }
    appendUtf8(word, static_cast<uint32_t>(scalar));
    handle_.clear();
    return ComposePlan::commit(std::move(word));
}

ComposePlan Composer::onKey(const KeyEvent &ev) {
    const KeyKind kind = classify(ev, settings_);
    // The toggle works in either mode — it is how the user gets back out of English.
    if (kind == KeyKind::Toggle) return toggleEnabled();
    if (!effectiveEnabled_) return ComposePlan::passThrough();

    switch (kind) {
    case KeyKind::Toggle: // handled above
        break;

    case KeyKind::Flip: {
        // The hotkey is always swallowed, but a flip that changed nothing (no word
        // composing) leaves the client alone.
        const FunputResult result = handle_.flipComposing();
        if (result.action == ACTION_NONE) return ComposePlan::swallow();
        if (nonPreedit_.on) return planFromResult(result);
        return ComposePlan::preedit(handle_.buffer());
    }

    case KeyKind::Shortcut:
        // Ctrl+A, Alt+Tab, Super+…: commit what is composing, then let the app have
        // the key — swallowing it would break the shortcut.
        return commitBuffer(false);

    case KeyKind::Backspace:
        // The composing word shortens and the key is swallowed. With nothing
        // composing the key passes through, so the app deletes its own character.
        if (handle_.buffer().empty()) return ComposePlan::passThrough();
        handle_.backspace();
        // Non-preedit: the character stands in the document, so the key passes
        // through and the app deletes its own — the engine is only kept in step.
        // Same division of labour as `ShellState::on_backspace` on Windows.
        if (nonPreedit_.on) return ComposePlan::passThrough();
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
        if (nonPreedit_.on) return planFromResult(result);
        // The engine can swallow a character without composing anything (a lone
        // modifier key in VNI, say); with nothing to show, the key passes through.
        if (handle_.buffer().empty()) return ComposePlan::passThrough();
        return ComposePlan::preedit(handle_.buffer());
    }
    }
    return ComposePlan::passThrough();
}

} // namespace funput
