// Who the focused client is, and what the writes to it travel through.
//
// A shell knows two things about a focus that the composer cannot see: a name for
// the app, so a non-preedit verdict can be remembered against it
// (nonpreedit/clients.h), and whether the writes reach the app directly or through
// the gnome-shell bridge, where a repair can be lost before it arrives
// (transport/guard.h). Both come from one string the framework hands the shell —
// Fcitx5's `InputContext::program()`, IBus's `focus-in-id` client — and this is the
// one place that string is read, so the two shells cannot drift on what `gnome-shell`
// means, or on which names stand for a toolkit rather than an app.
//
// This is also the seam a future source of identity plugs into: a focus bridge that
// resolves the real app behind the shell, or a data-driven client profile, would be
// another producer of a `ClientIdentity`, and nothing downstream would change. None
// of that exists; only the two producers below do.
//
// # Why `gnome-shell` is not a client name
//
// `program()` when it names one app, so a verdict survives clicking away and back. It
// is documented as possibly empty, and on GNOME/Wayland it is worse than empty: every
// client answers `gnome-shell`, the proxy they all speak through (see the measurements
// in README Linux). Remembering that name would stand the mode down for every app at
// once — curing one broken client by breaking the feature for everyone, which is the
// mistake this whole mechanism is built to avoid. Excluded by name rather than guessed
// at, the same way hidden_preedit.cpp names the clients it is about.
//
// The input context's uuid is not a way out. It used to stand in, as the narrower
// name, and it is not narrower: Fcitx5's own debug info on that desktop reads
// `Group [wayland:] has 1 InputContext(s)`, because gnome-shell holds one context for
// every app and refocuses it. A verdict filed under that uuid disabled the mode for
// Chrome too, until Fcitx5 restarted. Empty means a fresh question at every focus,
// the way IBus has it, and a verdict then lasts as long as the focus it was reached in.
//
// What the name cannot carry, the transport can: `gnome-shell` says nothing about
// *which* app is typing, but it says exactly which path the writes take.

#ifndef FUNPUT_COMPOSE_TRANSPORT_H
#define FUNPUT_COMPOSE_TRANSPORT_H

#include <cstdint>
#include <string>
#include <string_view>

namespace funput {

enum class Transport : uint8_t {
    // The shell talks to the app, or to a toolkit module inside it: X11, XWayland,
    // GTK_IM_MODULE=ibus, KDE. A delete the shell issues is the delete the app sees.
    Direct,
    // gnome-shell → mutter → text-input-v3 → the app. Each hop holds its own copy of
    // the surrounding text and converts a delete on that copy; a hop without one drops
    // the delete and keeps the commit. See transport/guard.h.
    GnomeShell,
};

struct ClientIdentity {
    // The app, or empty when nothing names exactly one — never remembered then.
    std::string name;
    Transport transport = Transport::Direct;
};

// The bare client strings that stand for a toolkit or a stand-in, never for one app:
// the shapes `focus-in-id` documents in ibusengine.c, less `gnome-shell`, which names
// the transport instead. Both producers meet them. IBus sees them as the client; Fcitx5
// sees them as `program()` whenever its IBus frontend passes the client string on
// unchanged — ibusfrontend.cpp `createInputContext` looks the process up only for
// `QIBusInputContext` and `gtk-im`, and asking for "the process" there is still a
// guess. `fake` is matched the way ibus-daemon tells its own stand-in contexts apart,
// by prefix: bus/inputcontext.c, `context->fake = (strncmp (client, "fake", 4) == 0)`.
inline bool namesAToolkit(std::string_view client) {
    return client.substr(0, 4) == "fake" || client == "xim" || client == "Qt" ||
           client == "QIBusInputContext" || client == "gtk-im";
}

// Fcitx5: `InputContext::program()`. A process name from its own frontends, or the IBus
// client string from the IBus one.
inline ClientIdentity identifyProgram(std::string_view program) {
    if (program == "gnome-shell") return {{}, Transport::GnomeShell};
    if (program.empty() || namesAToolkit(program)) return {};
    return {std::string(program), Transport::Direct};
}

// IBus: the `client` of `focus-in-id` (ibusengine.c documents the shapes). Only a
// toolkit module names the app — `gtk3-im:firefox`, `gtk4-im:gnome-text-editor` — so
// only `<module>:<program>` yields a name; every bare shape is one of `namesAToolkit()`'s
// or the bridge, and `gnome-shell` is what every GTK app reports on a GNOME Wayland
// session. What follows the colon is taken as it comes: GTK falls back to `(<pid>)`
// when the process has no name, and that still stands for one app for as long as it
// runs. Empty — the shell passes that for a plain `focus-in`, which is all a daemon
// older than 1.5.27 sends — names none either.
inline ClientIdentity identifyIBusClient(std::string_view client) {
    if (client == "gnome-shell") return {{}, Transport::GnomeShell};
    const size_t colon = client.find(':');
    if (colon == std::string_view::npos || namesAToolkit(client)) return {};
    const std::string_view program = client.substr(colon + 1);
    if (program.empty()) return {};
    return {std::string(program), Transport::Direct};
}

} // namespace funput

#endif // FUNPUT_COMPOSE_TRANSPORT_H
