#ifndef FUNPUT_IBUS_ENGINE_INTERNAL_H
#define FUNPUT_IBUS_ENGINE_INTERNAL_H

#include <ibus.h>

#include "engine.h"
#include "ffi_handle.h"
#include "settings.h"
#include "settings_watch.h"

namespace funput_ibus {

struct EngineState {
    funput::Handle handle;
    funput::Settings settings;
    bool effectiveEnabled = true;
    funput::SettingsWatcher watcher;
    guint watcherSource = 0;
};

EngineState *stateOf(IBusEngine *engine);
void applySettings(EngineState *state);
void updatePreedit(IBusEngine *engine, EngineState *state);
void commitBuffer(IBusEngine *engine, EngineState *state);
bool handleBoundary(IBusEngine *engine, EngineState *state, char32_t scalar,
                    funput::KeySource source = funput::KeySource::Standard);
bool matchesToggle(EngineState *state, guint keyval, guint modifiers);
bool matchesFlip(EngineState *state, guint keyval, guint modifiers);
void toggleEnabled(IBusEngine *engine, EngineState *state);

gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint keycode, guint modifiers);
void focusIn(IBusEngine *engine);
void focusOut(IBusEngine *engine);
void enable(IBusEngine *engine);
void disable(IBusEngine *engine);
void reset(IBusEngine *engine);

} // namespace funput_ibus

#endif
