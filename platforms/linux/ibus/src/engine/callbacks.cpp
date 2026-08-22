// The IBus callbacks: one keystroke, and the four ways a composition ends.

#include "engine/internal.h"

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
    // leaves the engine and the client describing different things. Deferring until
    // the word ends is also what lets `setNonPreedit()` be a bare assignment here,
    // where `applySettings()` has to clear the engine.
    if (state->composer.isComposing()) return;
    // The setting asks; the client decides. `sawSurroundingText` rather than the
    // capability bit for the reason spelled out in internal.h, and it can only become
    // true after the client has spoken — which is why this is re-evaluated on every
    // keystroke rather than settled once at focus-in the way the Fcitx5 shell can.
    state->composer.setNonPreedit(state->composer.settings().nonPreedit &&
                                  state->sawSurroundingText);
}

gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint, guint modifiers) {
    // Releases never reach the composer: nothing in the typing rules depends on
    // them, and each framework reports them differently.
    if (modifiers & IBUS_RELEASE_MASK) return FALSE;

    // Between words, re-ask whether this client can take a document repair. The answer
    // changes during a focus — surrounding text only starts arriving once the client
    // answers — so unlike Fcitx5 there is no single moment early enough to settle it.
    applyNonPreeditMode(engine);

    EngineState *state = stateOf(engine);
    const funput::KeyEvent ev = toKeyEvent(keyval, modifiers);
    // One read of the document, used twice: to check that the last repair landed, and
    // as the word a Backspace may re-open. Same wiring, same order, as the Fcitx5
    // shell's keyEvent().
    const bool nonPreedit = state->composer.nonPreedit();
    const std::string before = nonPreedit ? textBeforeCaret(engine) : std::string();
    if (nonPreedit) state->composer.observeDocument(before, hasSelection(engine));

    const bool reopen = state->composer.nonPreedit() && !state->composer.isComposing() &&
                        funput::classify(ev, state->composer.settings()) ==
                            funput::KeyKind::Backspace;

    const funput::ComposePlan plan = state->composer.onKey(ev);
    applyPlan(engine, plan);

    // Writes nothing itself: the key passes through, the app deletes its own
    // character, and the engine just takes ownership of the word left behind.
    if (reopen) state->composer.adoptWordBeforeBackspace(before);
    return plan.consumed ? TRUE : FALSE;
}

void focusIn(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    // A different client, which has said nothing yet. Whatever the last one did tells
    // us nothing about this one.
    state->sawSurroundingText = false;
    // A new client: forget whether the last one could be trusted with a repair. Kept
    // out of applyNonPreeditMode(), which runs every keystroke — clearing there would
    // wipe a verdict the moment it was reached.
    state->composer.onFocusChanged();
    requestSurroundingText(engine);
    if (state->composer.reloadSettingsIfChanged()) state->composer.applySettings();
    applyNonPreeditMode(engine);
    if (state->composer.settings().autoCapitalize) state->composer.armCapitalization();
}

void enable(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    if (state->composer.reloadSettingsIfChanged()) state->composer.applySettings();
    applyNonPreeditMode(engine);
    requestSurroundingText(engine);
}

void setSurroundingText(IBusEngine *engine, IBusText *text, guint cursorPos, guint anchorPos) {
    // Chain up first: the base class is what caches the text, so skipping this leaves
    // `ibus_engine_get_surrounding_text` returning nothing for the rest of the session.
    // Reached through `g_type_class_peek_parent` rather than G_DEFINE_TYPE's
    // `ibus_funput_engine_parent_class`, which is static to object.cpp.
    auto *parent = IBUS_ENGINE_CLASS(g_type_class_peek_parent(G_OBJECT_GET_CLASS(engine)));
    if (parent->set_surrounding_text != nullptr) {
        parent->set_surrounding_text(engine, text, cursorPos, anchorPos);
    }
    EngineState *state = stateOf(engine);
    state->sawSurroundingText = true;
    const gchar *raw = text != nullptr ? ibus_text_get_text(text) : nullptr;
    state->surroundingText = raw != nullptr ? raw : "";
    state->surroundingCursor = cursorPos;
    state->surroundingAnchor = anchorPos;
}

// Focus loss, reset and engine switch all commit the composing word — but IBus does
// it, not us: updatePreedit() publishes the preedit with IBUS_ENGINE_PREEDIT_COMMIT,
// and the daemon flushes it to the client before dispatching these callbacks.
// Committing again here would type the word twice, so only drop our own state.
void focusOut(IBusEngine *engine) {
    stateOf(engine)->composer.discard();
}

void disable(IBusEngine *engine) {
    stateOf(engine)->composer.discard();
}

void reset(IBusEngine *engine) {
    stateOf(engine)->composer.discard();
}

} // namespace funput_ibus
