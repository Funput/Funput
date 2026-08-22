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

} // namespace

void FunputEngine::keyEvent(const fcitx::InputMethodEntry &, fcitx::KeyEvent &event) {
    const bool released = event.isRelease();
    const funput::KeyEvent toggleEv = toKeyEvent(event.key());
    if (toggleChord_.feed(toggleEv, released, composer_.settings().toggleHotkey)) {
        applyPlan(event.inputContext(), composer_.toggleEnabled());
        refreshStatus(event.inputContext());
        event.filterAndAccept();
        return;
    }
    // Releases never reach the composer: nothing in the typing rules depends on
    // them, and each framework reports them differently.
    if (released) return;

    // Between words, re-ask whether this client can take a document repair. Surrounding
    // text often arrives only after the client answers — Calc, an empty GTK field —
    // so unlike the old focus-in snapshot there is no single moment early enough.
    applyNonPreeditMode();

    const funput::KeyEvent ev = toggleEv;
    // One read of the document, used twice. It lets the composer check that its last
    // repair actually landed — a client that takes commits but drops deletes turns
    // the mode off rather than appending to the user's text forever — and it is the
    // same text a Backspace needs below.
    const bool nonPreedit = composer_.nonPreedit();
    const std::string before = nonPreedit ? textBeforeCaret(event.inputContext()) : std::string();
    if (nonPreedit) composer_.observeDocument(before, hasSelection(event.inputContext()));

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
