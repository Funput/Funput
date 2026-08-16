// The IBus callbacks: one keystroke, and the four ways a composition ends.

#include "engine/internal.h"

#include <cstring>

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

gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint, guint modifiers) {
    // Releases never reach the composer: nothing in the typing rules depends on
    // them, and each framework reports them differently.
    if (modifiers & IBUS_RELEASE_MASK) return FALSE;

    EngineState *state = stateOf(engine);
    const funput::ComposePlan plan = state->composer.onKey(toKeyEvent(keyval, modifiers));
    applyPlan(engine, plan);
    return plan.consumed ? TRUE : FALSE;
}

void focusIn(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    // A different client, which has said nothing yet. Whatever the last one did tells
    // us nothing about this one.
    state->sawSurroundingText = false;
    if (state->composer.reloadSettingsIfChanged()) state->composer.applySettings();
    if (state->composer.settings().autoCapitalize) state->composer.armCapitalization();
    // No per-app VI/EN default here: unlike Fcitx5's InputContext::program(), IBus
    // hands the engine no app identity. See platforms/linux/README.md.
}

void enable(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    if (state->composer.reloadSettingsIfChanged()) state->composer.applySettings();
    // Yes, a "get" whose result is thrown away. Calling it here with null out-params
    // is how IBus takes an engine's registration for surrounding text — its own
    // documentation says it "must be called in the enable handler". Without it the
    // client never sends any, and everything that reads the document is dead. Fcitx5
    // needs no such handshake, which is why this has no counterpart over there.
    ibus_engine_get_surrounding_text(engine, nullptr, nullptr, nullptr);
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
    stateOf(engine)->sawSurroundingText = true;
    // Off unless G_MESSAGES_DEBUG asks for it. Two questions this answers on a real
    // session: whether the registration above worked at all (any line means yes), and
    // whether `cursorPos` counts characters or bytes — `tiếng` is 5 of one and 7 of
    // the other, and the IBus headers disagree with themselves about which it is.
    const gchar *raw = text != nullptr ? ibus_text_get_text(text) : nullptr;
    g_debug("funput: surrounding bytes=%zu cursor=%u anchor=%u",
            raw != nullptr ? strlen(raw) : 0, cursorPos, anchorPos);
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
