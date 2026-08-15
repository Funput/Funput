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
};

EngineState *stateOf(IBusEngine *engine);

// Perform one plan against the client. Swallowing the key is the caller's job —
// only processKeyEvent() can, by returning TRUE.
void applyPlan(IBusEngine *engine, const funput::ComposePlan &plan);
void updatePreedit(IBusEngine *engine, const std::string &text);

gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint keycode, guint modifiers);
void focusIn(IBusEngine *engine);
void focusOut(IBusEngine *engine);
void enable(IBusEngine *engine);
void disable(IBusEngine *engine);
void reset(IBusEngine *engine);

} // namespace funput_ibus

#endif
