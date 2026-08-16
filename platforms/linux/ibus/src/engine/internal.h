// IBus engine for Funput.
//
// A thin adapter over `funput::Composer` (platforms/linux/common/compose/), which
// holds the typing rules and is shared with the Fcitx5 shell. This file's job is
// translation only: IBus keyvals in, a `funput::ComposePlan` performed against the
// engine out.

#ifndef FUNPUT_IBUS_ENGINE_INTERNAL_H
#define FUNPUT_IBUS_ENGINE_INTERNAL_H

#include <string>

#include <ibus.h>

#include "compose/composer/composer.h"
#include "engine.h"
#include "settings/watch.h"

namespace funput_ibus {

struct EngineState {
    funput::Composer composer;
    funput::SettingsWatcher watcher;
    guint watcherSource = 0;
    // Whether the focused client has actually sent surrounding text, as opposed to
    // claiming it can. `IBUS_CAP_SURROUNDING_TEXT` sits right there in
    // `engine->client_capabilities` and is not used on purpose: the same flag on
    // Fcitx5 was absent on contexts where surrounding text worked perfectly and
    // present on contexts that answered nothing (platforms/linux/README.md). Having
    // received text is a fact; advertising support is a promise.
    //
    // Cleared on focus change, because it describes the client, not the engine. It
    // therefore cannot be true at focus-in the way Fcitx5's `isValid()` can — only
    // once the client has spoken at least once.
    bool sawSurroundingText = false;
};

EngineState *stateOf(IBusEngine *engine);

// Perform one plan against the client. Swallowing the key is the caller's job —
// only processKeyEvent() can, by returning TRUE.
void applyPlan(IBusEngine *engine, const funput::ComposePlan &plan);
void updatePreedit(IBusEngine *engine, const std::string &text);

// --- reading the document -----------------------------------------------------
//
// The IBus twins of `textBeforeCaret` and `hasSelection` in
// fcitx5/src/funput_client.cpp; same names so both shells read alike. Both return
// the "nothing usable" answer when the client has never sent surrounding text.

std::string textBeforeCaret(IBusEngine *engine);
bool hasSelection(IBusEngine *engine);

// Decide whether the composer may build the word in the document. Must run after
// every `applySettings()`, which seeds the mode from the setting alone — and the
// setting is shared with the Fcitx5 shell, which is the only one that can perform an
// `Effect::Replace` today.
void applyNonPreeditMode(IBusEngine *engine);

gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint keycode, guint modifiers);
void focusIn(IBusEngine *engine);
void focusOut(IBusEngine *engine);
void enable(IBusEngine *engine);
void disable(IBusEngine *engine);
void reset(IBusEngine *engine);
void setSurroundingText(IBusEngine *engine, IBusText *text, guint cursorPos, guint anchorPos);

} // namespace funput_ibus

#endif
