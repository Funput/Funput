#include "engine/internal.h"

#include <glib-unix.h>

struct _IBusFunputEngine {
    IBusEngine parent;
    funput_ibus::EngineState *state;
};

struct _IBusFunputEngineClass {
    IBusEngineClass parent;
};

G_DEFINE_TYPE(IBusFunputEngine, ibus_funput_engine, IBUS_TYPE_ENGINE)

namespace funput_ibus {

EngineState *stateOf(IBusEngine *engine) {
    return FUNPUT_ENGINE(engine)->state;
}

} // namespace funput_ibus

namespace {

gboolean settingsChanged(gint, GIOCondition, gpointer data) {
    auto *state = FUNPUT_ENGINE(data)->state;
    if (state->watcher.drain() && state->composer.reloadSettings()) {
        state->composer.applySettings();
    }
    return G_SOURCE_CONTINUE;
}

} // namespace

static void ibus_funput_engine_init(IBusFunputEngine *self) {
    // The composer pushes its (default) settings into the engine as it is built;
    // the file itself is read on the first focusIn()/enable().
    self->state = new funput_ibus::EngineState();
    if (self->state->watcher.fd() >= 0) {
        self->state->watcherSource =
            g_unix_fd_add(self->state->watcher.fd(), G_IO_IN, settingsChanged, self);
    }
}

static void finalize(GObject *object) {
    auto *self = FUNPUT_ENGINE(object);
    if (self->state && self->state->watcherSource != 0) {
        g_source_remove(self->state->watcherSource);
    }
    delete self->state;
    self->state = nullptr;
    G_OBJECT_CLASS(ibus_funput_engine_parent_class)->finalize(object);
}

static void ibus_funput_engine_class_init(IBusFunputEngineClass *klass) {
    GObjectClass *object = G_OBJECT_CLASS(klass);
    IBusEngineClass *engine = IBUS_ENGINE_CLASS(klass);
    object->finalize = finalize;
    engine->process_key_event = funput_ibus::processKeyEvent;
    engine->focus_in = funput_ibus::focusIn;
    engine->focus_out = funput_ibus::focusOut;
    engine->enable = funput_ibus::enable;
    engine->disable = funput_ibus::disable;
    engine->reset = funput_ibus::reset;
}
