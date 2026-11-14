// The Fcitx5 side of one keystroke: normalize the key, hand it to the composer, then
// let funput_client.cpp perform the plan. The typing rules themselves live in
// common/compose/.

#include "funput_engine.h"

#include <utility>

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
    const funput::KeyEvent ev = toKeyEvent(event.key());
    funput::Trace &trace = funput::Trace::shared();
    if (toggleChord_.feed(ev, released, composer_.settings().toggleHotkey)) {
        applyPlan(event.inputContext(), composer_.toggleEnabled());
        trace.note(composer_.enabled() ? "toggle: vi" : "toggle: en");
        refreshStatus(event.inputContext());
        event.filterAndAccept();
        return;
    }
    // Releases never reach the composer: nothing in the typing rules depends on
    // them, and each framework reports them differently.
    if (released) return;

    // Pure, so asking once up front for both the trace and the Backspace test below
    // changes nothing the composer decides.
    const funput::KeyKind kind = funput::classify(ev, composer_.settings());
    if (trace.enabled()) trace.key(kind, ev);

    // Between words, re-ask whether this client can take a document repair. Surrounding
    // text often arrives only after the client answers — Calc, an empty GTK field —
    // so unlike the old focus-in snapshot there is no single moment early enough.
    applyNonPreeditMode();

    // One read of the document, before the key. It lets the composer check that its
    // last repairs actually landed — a client that takes commits but drops deletes
    // turns the mode off rather than appending to the user's text forever — and it
    // is all the composer needs: the word a Backspace may re-open below comes from
    // the document as the composer predicts it, since this reading can be a
    // keystroke old. tests/compose/composer/nonpreedit/sim/client.h mirrors this
    // sequence step for step; a change here is a change there.
    const bool nonPreedit = composer_.nonPreedit();
    const std::string before = nonPreedit ? textBeforeCaret(event.inputContext()) : std::string();
    const bool answered = std::exchange(surroundingFresh_, false);
    if (nonPreedit) {
        fcitx::InputContext *ic = event.inputContext();
        const bool selection = hasSelection(ic);
        const uint32_t after = selectedAfter(ic);
        const funput::Observation seen = composer_.observeDocument(before, selection, answered, after);
        if (trace.enabled()) trace.reading(before, answered, selection, after, seen);
    }

    // A Backspace with nothing composing is about to eat a *committed* character. In
    // non-preedit the word it lands in is right there in the document, so the composer
    // is offered it and decides whether it is worth re-opening (a syllable, a trigger).
    // Asking `classify()` rather than testing the keysym keeps Ctrl+Backspace (delete
    // word) out of this: that is a shortcut, not a Backspace.
    const bool reopen = composer_.nonPreedit() && !composer_.isComposing() &&
                        kind == funput::KeyKind::Backspace;

    const funput::ComposePlan plan = composer_.onKey(ev);
    applyPlan(event.inputContext(), plan);
    if (trace.enabled()) trace.plan(plan);
    if (plan.consumed) event.filterAndAccept();

    // After the plan, and never writing anything itself: the character is deleted on
    // whichever channel the plan chose, and the engine simply takes ownership of what
    // the composer predicts is left so the next keystroke can edit it.
    if (reopen) {
        const bool taken = composer_.adoptWordBeforeBackspace();
        if (trace.enabled()) trace.adopt(taken);
    }
}
