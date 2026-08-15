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
    if (state->composer.reloadSettingsIfChanged()) state->composer.applySettings();
    if (state->composer.settings().autoCapitalize) state->composer.armCapitalization();
    // No per-app VI/EN default here: unlike Fcitx5's InputContext::program(), IBus
    // hands the engine no app identity. See platforms/linux/README.md.
}

void enable(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    if (state->composer.reloadSettingsIfChanged()) state->composer.applySettings();
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
