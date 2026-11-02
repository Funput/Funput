// Who the focused client is, and what the writes to it travel through.
//
// A shell knows two things about a focus that the composer cannot see: a name for
// the app, so a non-preedit verdict can be remembered against it
// (nonpreedit/clients.h), and whether the writes reach the app directly or through
// the gnome-shell bridge, where a repair can be lost before it arrives
// (transport/guard.h). Both come from one string the framework hands the shell —
// Fcitx5's `InputContext::program()`, IBus's `focus-in-id` client — and this is the
// one place that string is read, so the two shells cannot drift on what `gnome-shell`
// means.
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

// Fcitx5: `InputContext::program()`.
inline ClientIdentity identifyProgram(std::string_view program) {
    if (program == "gnome-shell") return {{}, Transport::GnomeShell};
    return {std::string(program), Transport::Direct};
}

// IBus: the `client` of `focus-in-id` (ibusengine.c documents the shapes). Only a
// toolkit module names the app — `gtk3-im:firefox`, `gtk4-im:gnome-text-editor` — so
// only `<module>:<program>` yields a name. `gnome-shell` is the bridge, as above, and
// on a GNOME Wayland session it is what every GTK app reports. `fake` is IBus's own
// stand-in when nothing has focus, and `xim`, `QIBusInputContext` and the like are a
// toolkit, not an app; all of those would stand for more than one app, so they name
// none. Wired up by the shell in a later PR; defined here so the rule has one home.
inline ClientIdentity identifyIBusClient(std::string_view client) {
    if (client == "gnome-shell") return {{}, Transport::GnomeShell};
    if (client.substr(0, 4) == "fake") return {};
    const size_t colon = client.find(':');
    if (colon == std::string_view::npos) return {};
    return {std::string(client.substr(colon + 1)), Transport::Direct};
}

} // namespace funput

#endif // FUNPUT_COMPOSE_TRANSPORT_H
