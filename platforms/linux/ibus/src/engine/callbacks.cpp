// The IBus callback for one keystroke, and the mode decision it begins with. The
// focus, enable and reset callbacks: focus.cpp.

#include "engine/internal.h"

#include <utility>

#include "trace/trace.h"

namespace funput_ibus {

namespace {

// IBus's keyval + modifier mask -> the composer's normalized key. IBus keyvals are
// X11 keysyms, the same values Fcitx5 uses, so they pass straight through; only the
// modifier masks and the keyval-to-Unicode mapping are IBus's own.
funput::KeyEvent toKeyEvent(guint keyval, guint modifiers) {
    funput::KeyEvent ev;
    ev.keysym = keyval;
    ev.ch = static_cast<char32_t>(ibus_keyval_to_unicode(keyval));
    ev.mods.ctrl = (modifiers & IBUS_CONTROL_MASK) != 0;
    ev.mods.alt = (modifiers & IBUS_MOD1_MASK) != 0;
    ev.mods.super = (modifiers & IBUS_SUPER_MASK) != 0;
    ev.mods.shift = (modifiers & IBUS_SHIFT_MASK) != 0;
    return ev;
}

} // namespace

// Yes, a "get" whose result is thrown away: with null out-params this is how IBus
// registers an engine's interest in surrounding text. Its docs say the enable handler,
// but that fires once while the request turns out to be per input context — a
// daemon-spawned engine that only asked there received nothing at all. So ask again on
// every focus. Fcitx5 needs no such handshake, hence no counterpart over there.
void requestSurroundingText(IBusEngine *engine) {
    ibus_engine_get_surrounding_text(engine, nullptr, nullptr, nullptr);
}

void applyNonPreeditMode(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    // Never mid-word. The two modes disagree about where the composing word lives —
    // one has it in a preedit, the other in the document — so flipping under one
    // leaves the engine and the client describing different things.
    if (state->composer.isComposing()) return;
    // The client decides. `sawSurroundingText` rather than the capability bit for the
    // reason spelled out in internal.h, and it can only become true after the client
    // has spoken — which is why this is re-evaluated on every keystroke rather than
    // settled once at focus-in the way the Fcitx5 shell can.
    const bool wasNonPreedit = state->composer.nonPreedit();
    // And the path to it must be able to carry a repair right now: on the gnome-shell
    // bridge the first word in an empty field, or the first after a click, would lose
    // its repairs for certain (transport/guard.h), so that one word is composed in a
    // preedit. The transport is what focus.cpp read off the `focus-in-id` client; an
    // unnamed focus (a plain `focus-in`, all a daemon older than 1.5.27 sends) is
    // `Transport::Direct`, and the guard then stays inert. Same place as Fcitx5's, and
    // the document is decoded for it only on the bridge: this runs on every key.
    const bool bridge =
        state->composer.transport() == funput::Transport::GnomeShell &&
        state->composer.bridgeBlocksRepairs(state->spokeSinceReset, textBeforeCaret(engine));
    state->composer.setNonPreedit(state->sawSurroundingText && !bridge);
    funput::Trace &trace = funput::Trace::shared();
    if (trace.enabled() && state->composer.nonPreedit() != wasNonPreedit) {
        // Off although the client has spoken is the composer's own latch: it was
        // caught dropping a delete, and that verdict already stands in the trace.
        const char *reason = state->composer.nonPreedit() ? "surrounding"
                             : !state->sawSurroundingText  ? "silent"
                             : bridge                      ? "bridge"
                                                           : "refused";
        trace.mode(state->composer.nonPreedit(), reason);
    }
}

gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint, guint modifiers) {
    EngineState *state = stateOf(engine);
    const bool released = (modifiers & IBUS_RELEASE_MASK) != 0;
    const funput::KeyEvent ev = toKeyEvent(keyval, modifiers);
    funput::Trace &trace = funput::Trace::shared();
    if (state->toggleChord.feed(ev, released, state->composer.settings().toggleHotkey)) {
        applyPlan(engine, state->composer.toggleEnabled());
        trace.note(state->composer.enabled() ? "toggle: vi" : "toggle: en");
        return TRUE;
    }
    // Releases never reach the composer: nothing in the typing rules depends on
    // them, and each framework reports them differently.
    if (released) return FALSE;

    // Pure, so asking once up front for both the trace and the Backspace test below
    // changes nothing the composer decides.
    const funput::KeyKind kind = funput::classify(ev, state->composer.settings());
    if (trace.enabled()) trace.key(kind, ev);

    // Between words, re-ask whether this client can take a document repair.
    // Surrounding text often arrives only after the client answers.
    applyNonPreeditMode(engine);

    // One read of the document, before the key, to check that the last repairs
    // landed. The word a Backspace may re-open comes from the composer's own
    // prediction, not from this reading, which can be a keystroke old. Same order as
    // Fcitx5's keyEvent(), and as the ClientSim test double mirrors it.
    const bool nonPreedit = state->composer.nonPreedit();
    const std::string before = nonPreedit ? textBeforeCaret(engine) : std::string();
    const bool answered = std::exchange(state->surroundingFresh, false);
    if (nonPreedit) {
        const bool selection = hasSelection(engine);
        const uint32_t after = selectedAfter(engine);
        const funput::Observation seen =
            state->composer.observeDocument(before, selection, answered, after);
        if (trace.enabled()) trace.reading(before, answered, selection, after, seen);
    }

    const bool reopen = state->composer.nonPreedit() && !state->composer.isComposing() &&
                        kind == funput::KeyKind::Backspace;

    const funput::ComposePlan plan = state->composer.onKey(ev);
    applyPlan(engine, plan);
    if (trace.enabled()) trace.plan(plan);

    // Nothing is written here; the engine only takes ownership of what is left.
    if (reopen) {
        const bool taken = state->composer.adoptWordBeforeBackspace();
        if (trace.enabled()) trace.adopt(taken);
    }
    return plan.consumed ? TRUE : FALSE;
}

} // namespace funput_ibus
