// Who a focus is, from the one string each framework hands the shell.
//
// The name is what a non-preedit verdict is remembered against, so a string that
// stands for more than one app must never become one: `gnome-shell` on GNOME/Wayland
// is every app at once, and a verdict filed under it would stand the mode down for all
// of them. The transport is the second thing that string says, and the only thing
// `gnome-shell` says.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/verdicts/refusal.h"
#include "compose/transport.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("Fcitx5: the program names the app, except the one that is every app") {
    const ClientIdentity firefox = identifyProgram("firefox");
    CHECK(firefox.name == "firefox");
    CHECK(firefox.transport == Transport::Direct);

    const ClientIdentity shell = identifyProgram("gnome-shell");
    CHECK(shell.name.empty());
    CHECK(shell.transport == Transport::GnomeShell);

    CHECK(identifyProgram("").name.empty());
    CHECK(identifyProgram("").transport == Transport::Direct);
}

TEST_CASE("IBus: only a toolkit module names the app") {
    // The shapes ibusengine.c documents for `focus-in-id`.
    CHECK(identifyIBusClient("gtk3-im:firefox").name == "firefox");
    CHECK(identifyIBusClient("gtk3-im:firefox").transport == Transport::Direct);
    CHECK(identifyIBusClient("gtk4-im:gnome-text-editor").name == "gnome-text-editor");
    CHECK(identifyIBusClient("gtk-im:gimp").name == "gimp");

    const ClientIdentity shell = identifyIBusClient("gnome-shell");
    CHECK(shell.name.empty());
    CHECK(shell.transport == Transport::GnomeShell);

    // A module that names nothing is nothing.
    CHECK(identifyIBusClient("gtk3-im:").name.empty());
    CHECK(identifyIBusClient(":").name.empty());
}

TEST_CASE("the names that stand for a toolkit are the same list for both producers") {
    // IBus's stand-in when nothing has focus, and the toolkits that name no app, as
    // the IBus engine meets them in `focus-in-id` and as Fcitx5 meets them in
    // `program()` when its IBus frontend passes the client string on. Both must
    // refuse every one, or a verdict against `xim` would stand the mode down for
    // every X11 app at once on one shell and not the other.
    for (const char *generic :
         {"fake", "fake-gtk-im", "fake:x", "xim", "QIBusInputContext", "Qt", "gtk-im", ""}) {
        CAPTURE(generic);
        CHECK((generic[0] == '\0') != namesAToolkit(generic));
        for (const ClientIdentity &client : {identifyIBusClient(generic), identifyProgram(generic)}) {
            CHECK(client.name.empty());
            CHECK(client.transport == Transport::Direct);
        }
    }
    // `fake` by prefix, as ibus-daemon recognises its own contexts (bus/inputcontext.c
    // `strncmp (client, "fake", 4)`); a name that merely starts like one is a name.
    CHECK(namesAToolkit("fakeapp"));
    CHECK_FALSE(namesAToolkit("fa"));
    CHECK(identifyProgram("fa").name == "fa");
    // A module-qualified client through Fcitx5's IBus frontend is still one app.
    CHECK(identifyProgram("gtk3-im:firefox").name == "gtk3-im:firefox");
}

TEST_CASE("a refused client re-focused as the bridge is a fresh question") {
    Composer composer = composerFor(Method::Telex);
    composer.onFocusChanged(identifyProgram("cursor"));
    composer.setNonPreedit(true);
    typeUntilRefused(composer, "tieengs", applyIgnoringDeletes);
    REQUIRE_FALSE(composer.nonPreedit());

    // The same editor reached through gnome-shell answers `gnome-shell`, as every app
    // there does. The verdict was reached under `cursor`, and that name is not this
    // one, so the mode is tried again — costing one word, not every app's mode.
    composer.onFocusChanged(identifyProgram("gnome-shell"));
    composer.setNonPreedit(true);
    CHECK(composer.nonPreedit());

    // And `cursor` itself still remembers.
    composer.onFocusChanged(identifyProgram("cursor"));
    composer.setNonPreedit(true);
    CHECK_FALSE(composer.nonPreedit());
}
