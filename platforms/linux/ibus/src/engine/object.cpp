#include "engine/internal.h"

#include <glib-unix.h>

#include "trace/trace.h"

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

void settingsReloaded(EngineState *state) {
    // One trace per process, however many engines ibus-daemon asks this process for.
    // Every engine re-reads the same file, and each applies what it just read: the
    // first to reload after an edit moves the switch, the rest find nothing to
    // change. Only ever from a file that was read — a composer's defaults say
    // `trace: false`, and would switch off a trace another engine turned on.
    funput::Trace::shared().configure(state->composer.settings());
    state->composer.applySettings();
}

} // namespace funput_ibus

namespace {

gboolean settingsChanged(gint, GIOCondition, gpointer data) {
    auto *state = FUNPUT_ENGINE(data)->state;
    if (state->watcher.drain() && state->composer.reloadSettings()) {
        funput_ibus::settingsReloaded(state);
    }
    return G_SOURCE_CONTINUE;
}

} // namespace

static void ibus_funput_engine_init(IBusFunputEngine *self) {
    // The file itself is read on the first focusIn()/enable() and reaches the trace
    // through settingsReloaded(). Until then the trace is whatever FUNPUT_TRACE says,
    // applied by shared() itself — for an engine started by hand, since
    // ibus-daemon's own has none. Naming the shell again is a no-op.
    self->state = new funput_ibus::EngineState();
    funput::Trace::shared().start("ibus");
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
    engine->set_surrounding_text = funput_ibus::setSurroundingText;
}
