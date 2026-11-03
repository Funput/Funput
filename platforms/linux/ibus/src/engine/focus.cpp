// The IBus callbacks around a keystroke: a client gained or lost focus, the engine
// was enabled or disabled, the client answered with surrounding text, or IBus reset
// the composition. One keystroke itself: callbacks.cpp.

#include "engine/internal.h"

#include "trace/trace.h"

namespace funput_ibus {

void focusIn(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    // No name to give: IBus does not say which client this is (the README's "IBus
    // không biết tên client"). The line still marks where one focus ends and the
    // next begins.
    funput::Trace::shared().focus({});
    // A different client, which has said nothing yet. Whatever the last one did tells
    // us nothing about this one.
    state->sawSurroundingText = false;
    state->surroundingFresh = false;
    state->spokeSinceReset = false;
    state->toggleChord.reset();
    // A new client: forget whether the last one could be trusted with a repair. Kept
    // out of applyNonPreeditMode(), which runs every keystroke — clearing there would
    // wipe a verdict the moment it was reached. Unnamed, unlike Fcitx5: IBus gives an
    // engine per input context and calls this only on a real focus change — and so
    // on `Transport::Direct`, which keeps the bridge guard inert here until the
    // client is named (compose/transport.h, `identifyIBusClient`).
    state->composer.onFocusChanged();
    requestSurroundingText(engine);
    if (state->composer.reloadSettingsIfChanged()) settingsReloaded(state);
    applyNonPreeditMode(engine);
    if (state->composer.settings().autoCapitalize) state->composer.armCapitalization();
}

void enable(IBusEngine *engine) {
    EngineState *state = stateOf(engine);
    if (state->composer.reloadSettingsIfChanged()) settingsReloaded(state);
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
    state->surroundingFresh = true;
    state->spokeSinceReset = true;
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
    funput::Trace::shared().note("focus-out");
    stateOf(engine)->composer.discard();
}

void disable(IBusEngine *engine) {
    stateOf(engine)->composer.discard();
}

void reset(IBusEngine *engine) {
    funput::Trace::shared().note("reset");
    EngineState *state = stateOf(engine);
    // On the gnome-shell bridge a reset is a click into the field, after which the
    // bridge holds no surrounding text until the client answers again
    // (transport/guard.h). Count the answers from here. The word is committed by
    // then: by mutter on the bridge, elsewhere by ibus-daemon's `_ic_reset` or the
    // client that asked to commit the preedit itself.
    state->spokeSinceReset = false;
    state->composer.discard();
}

} // namespace funput_ibus
