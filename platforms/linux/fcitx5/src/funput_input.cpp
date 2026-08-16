// The Fcitx5 side of one keystroke: normalize the key, hand it to the composer, then
// let funput_client.cpp perform the plan. The typing rules themselves live in
// common/compose/.

#include "funput_engine.h"

namespace {

// Fcitx5's key -> the composer's normalized one. Its keysyms are X11 keysyms, the
// same values IBus uses, so they pass straight through; only the modifier set and
// the keysym-to-Unicode mapping are Fcitx5's own.
funput::KeyEvent toKeyEvent(const fcitx::Key &key) {
    funput::KeyEvent ev;
    ev.keysym = static_cast<uint32_t>(key.sym());
    ev.ch = static_cast<char32_t>(fcitx::Key::keySymToUnicode(key.sym()));
    const auto states = key.states();
    ev.mods.ctrl = states.test(fcitx::KeyState::Ctrl);
    ev.mods.alt = states.test(fcitx::KeyState::Alt);
    ev.mods.super = states.test(fcitx::KeyState::Super);
    ev.mods.shift = states.test(fcitx::KeyState::Shift);
    return ev;
}

// The document in front of the caret, as UTF-8. Fcitx5 reports the cursor in
// characters while the text is UTF-8, so the two have to be reconciled here —
// slicing by the cursor as if it were a byte offset cuts Vietnamese in half.
std::string textBeforeCaret(fcitx::InputContext *context) {
    const auto &surrounding = context->surroundingText();
    if (!surrounding.isValid()) return {};
    const std::vector<uint32_t> chars = funput::decodeUtf8(surrounding.text());
    std::string out;
    for (size_t i = 0; i < chars.size() && i < surrounding.cursor(); ++i) {
        funput::appendUtf8(out, chars[i]);
    }
    return out;
}

} // namespace

void FunputEngine::keyEvent(const fcitx::InputMethodEntry &, fcitx::KeyEvent &event) {
    // Releases never reach the composer: nothing in the typing rules depends on
    // them, and each framework reports them differently.
    if (event.isRelease()) return;

    const funput::KeyEvent ev = toKeyEvent(event.key());
    // One read of the document, used twice. It lets the composer check that its last
    // repair actually landed — a client that takes commits but drops deletes turns
    // the mode off rather than appending to the user's text forever — and it is the
    // same text a Backspace needs below.
    const bool nonPreedit = composer_.nonPreedit();
    const std::string before = nonPreedit ? textBeforeCaret(event.inputContext()) : std::string();
    if (nonPreedit) composer_.observeDocument(before);

    // A Backspace with nothing composing is about to eat a *committed* character. In
    // non-preedit the word it lands in is right there in the document, so the composer
    // is offered it and decides whether it is a Vietnamese syllable worth re-opening.
    // Asking `classify()` rather than testing the keysym keeps Ctrl+Backspace (delete
    // word) out of this: that is a shortcut, not a Backspace.
    const bool reopen = composer_.nonPreedit() && !composer_.isComposing() &&
                        funput::classify(ev, composer_.settings()) == funput::KeyKind::Backspace;

    const funput::ComposePlan plan = composer_.onKey(ev);
    applyPlan(event.inputContext(), plan);
    if (plan.consumed) event.filterAndAccept();

    // After the plan, and never writing anything itself: the key passes through, the
    // app deletes its own character, and the engine simply takes ownership of what is
    // left so the next keystroke can edit it.
    if (reopen) composer_.adoptWordBeforeBackspace(before);
}
