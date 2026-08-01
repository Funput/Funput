#include "engine_internal.h"

#include "boundary.h"

namespace funput_ibus {

gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint, guint modifiers) {
    EngineState *state = stateOf(engine);
    if (modifiers & IBUS_RELEASE_MASK) return FALSE;
    if (matchesToggle(state, keyval, modifiers)) {
        toggleEnabled(engine, state);
        return TRUE;
    }
    if (!state->effectiveEnabled) return FALSE;
    if (matchesFlip(state, keyval, modifiers)) {
        if (state->handle.flipComposing().action != ACTION_NONE) updatePreedit(engine, state);
        return TRUE;
    }
    if (modifiers & (IBUS_CONTROL_MASK | IBUS_MOD1_MASK | IBUS_SUPER_MASK)) {
        commitBuffer(engine, state);
        return FALSE;
    }
    if (keyval == IBUS_KEY_BackSpace) {
        if (!state->handle.buffer().empty()) {
            state->handle.backspace();
            updatePreedit(engine, state);
            return TRUE;
        }
        return FALSE;
    }
    const guint32 scalar = ibus_keyval_to_unicode(keyval);
    if (scalar == 0) {
        if (state->settings.autoCapitalize && keyval == IBUS_KEY_Return) {
            state->handle.armCapitalization();
        }
        commitBuffer(engine, state);
        return FALSE;
    }
    // A numpad digit is a literal number, not a VNI tone/shape: commit the current
    // word and emit the digit, like a word boundary (mirrors the macOS shell).
    if (funput::isNumpadDigitKeysym(keyval)) {
        return handleBoundary(engine, state, static_cast<char32_t>(scalar),
                              funput::KeySource::Numpad)
                   ? TRUE
                   : FALSE;
    }
    if (funput::isBoundary(static_cast<char32_t>(scalar), state->settings.method)) {
        return handleBoundary(engine, state, static_cast<char32_t>(scalar)) ? TRUE : FALSE;
    }
    state->handle.process(scalar);
    if (state->handle.buffer().empty()) return FALSE;
    updatePreedit(engine, state);
    return TRUE;
}

void focusIn(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    if (state->settings.reloadIfChanged()) applySettings(state);
    if (state->settings.autoCapitalize) state->handle.armCapitalization();
}

void enable(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    if (state->settings.reloadIfChanged()) applySettings(state);
}

// Focus loss, reset and engine switch all commit the composing word — but IBus does
// it, not us: updatePreedit() publishes the preedit with IBUS_ENGINE_PREEDIT_COMMIT,
// and the daemon flushes it to the client before dispatching these callbacks.
// Committing again here would type the word twice, so only drop our own state.
void focusOut(IBusEngine *engine) {
    stateOf(engine)->handle.clear();
}

void disable(IBusEngine *engine) {
    stateOf(engine)->handle.clear();
}

void reset(IBusEngine *engine) {
    stateOf(engine)->handle.clear();
}

} // namespace funput_ibus
