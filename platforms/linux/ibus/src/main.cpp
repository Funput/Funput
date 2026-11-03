// Process entry point for the Funput IBus engine. ibus-daemon launches this
// binary (per the <exec> in the installed component XML) with `--ibus`; it then
// connects to the bus, registers an IBusFactory bound to IBusFunputEngine, and runs
// the GLib main loop. `--xml` prints the component manifest and exits (a smoke
// test that needs no running IBus session).

#include <ibus.h>

#include <cstdio>
#include <cstring>

#include "engine.h"

#ifndef FUNPUT_VERSION
#define FUNPUT_VERSION "0.0.0"
#endif
#ifndef FUNPUT_IBUS_ENGINE_PATH
#define FUNPUT_IBUS_ENGINE_PATH "ibus-engine-funput"
#endif

namespace {

IBusBus *g_bus = nullptr;
IBusFactory *g_factory = nullptr;

// The component descriptor: identity + how ibus-daemon launches us, plus the one
// engine we expose. Used both for standalone registration and `--xml`. Mirrors
// data/funput.xml.in (both derive their exec path/version from the same CMake vars).
IBusComponent *makeComponent() {
    IBusComponent *component = ibus_component_new(
        "org.freedesktop.IBus.Funput",
        "Funput Vietnamese Input Method (Telex, Full Telex & VNI)",
        FUNPUT_VERSION,
        "MIT",
        "Funput",
        "https://github.com/Funput/Funput",
        FUNPUT_IBUS_ENGINE_PATH " --ibus",
        "funput");

    IBusEngineDesc *engine = ibus_engine_desc_new(
        "funput",                  // name
        "Funput",                  // longname
        "Vietnamese Telex, Full Telex & VNI",  // description
        "vi",                      // language
        "MIT",                     // license
        "Funput",                 // author
        "funput",                  // icon
        "us");                     // keyboard layout

    ibus_component_add_engine(component, engine);
    return component;
}

void onDisconnected(IBusBus * /*bus*/, gpointer /*user_data*/) {
    ibus_quit();
}

#if FUNPUT_IBUS_FOCUS_ID
// The daemon's CreateEngine. Not one engine per input context: under the default
// use-global-engine one instance moves from context to context — the daemon detaches
// it from the old one and attaches it to the new (bus/ibusimpl.c
// `bus_ibus_impl_set_focused_context`), pairing every focus-in with a focus-out on
// the same proxy — and asks for a fresh one when the global engine is switched away
// and back. The factory's own handler (`ibus_factory_real_create_engine`) builds the
// engine with `ibus_engine_new_with_type`, which never sets `has-focus-id`; the
// property is construct-only, so the only way to ask for `focus-in-id` is to construct
// the engine here, with the same three properties that function passes plus that one.
// Run before the class handler (G_SIGNAL_RUN_LAST), and the accumulator stops at the
// first non-null engine. A name that is not ours is left to the class handler, which
// answers the daemon "Cannot find engine" the way it always has.
//
// Returned floating, as `g_object_new` on an IBusObject makes it: the signal copies
// the value out, and the factory then `g_object_ref_sink`s what it receives, so an
// owned reference here would be one too many. The path only has to be unique on this
// connection, like the factory's own `/org/freedesktop/IBus/Engine/<n>`.
IBusEngine *createEngine(IBusFactory *factory, const gchar *name, gpointer /*user_data*/) {
    if (g_strcmp0(name, "funput") != 0) return nullptr;
    static guint serial = 0;
    gchar *path = g_strdup_printf("/org/freedesktop/IBus/Engine/Funput/%u", ++serial);
    auto *engine = static_cast<IBusEngine *>(g_object_new(
        FUNPUT_TYPE_ENGINE, "engine-name", name, "object-path", path, "connection",
        ibus_service_get_connection(IBUS_SERVICE(factory)), "has-focus-id", TRUE, nullptr));
    g_free(path);
    return engine;
}
#endif

// Register the factory and either request our well-known name (when started by
// ibus-daemon) or register the component on the fly (standalone debugging).
void startEngine(gboolean execByIbus) {
    ibus_init();

    g_bus = ibus_bus_new();
    g_object_ref_sink(g_bus);
    g_signal_connect(g_bus, "disconnected", G_CALLBACK(onDisconnected), nullptr);

    g_factory = ibus_factory_new(ibus_bus_get_connection(g_bus));
    g_object_ref_sink(g_factory);
    // The table of names this factory answers to. With `createEngine` connected it is
    // consulted only for a name that handler declines; without it, it is how the
    // engine gets built at all.
    ibus_factory_add_engine(g_factory, "funput", FUNPUT_TYPE_ENGINE);
#if FUNPUT_IBUS_FOCUS_ID
    g_signal_connect(g_factory, "create-engine", G_CALLBACK(createEngine), nullptr);
#endif

    if (execByIbus) {
        ibus_bus_request_name(g_bus, "org.freedesktop.IBus.Funput", 0);
    } else {
        ibus_bus_register_component(g_bus, makeComponent());
    }

    ibus_main();
}

} // namespace

int main(int argc, char **argv) {
    gboolean execByIbus = FALSE;

    for (int i = 1; i < argc; ++i) {
        if (std::strcmp(argv[i], "--xml") == 0) {
            IBusComponent *component = makeComponent();
            GString *out = g_string_new(nullptr);
            ibus_component_output(component, out, 4);
            std::printf("%s\n", out->str);
            g_string_free(out, TRUE);
            return 0;
        }
        if (std::strcmp(argv[i], "--ibus") == 0) {
            execByIbus = TRUE;
        }
    }

    startEngine(execByIbus);
    return 0;
}
