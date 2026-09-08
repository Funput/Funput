// Who a non-preedit verdict belongs to.
//
// The mode stands itself down when a client drops a `deleteSurroundingText`, and that
// verdict used to last exactly one focus. Fcitx5 calls `activate()` on a capability
// change as well as on a focus change, so a client that churns capabilities — Cursor's
// editor, whose suggestion widgets do — was forgiven every keystroke and mangled the
// next word, over and over. These lock the verdict to the client instead.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/refusal.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

// A composer that has already caught `client` dropping a delete.
Composer refusedBy(const char *client) {
    Composer composer = composerFor(Method::Telex);
    composer.onFocusChanged(client);
    composer.setNonPreedit(true);
    typeUntilRefused(composer, "tieengs", applyIgnoringDeletes);
    REQUIRE_FALSE(composer.nonPreedit());
    return composer;
}

// Re-focus `client` and ask for the mode back, as every shell does after an activate.
bool modeAfterFocusing(Composer &composer, const char *client) {
    composer.onFocusChanged(client);
    composer.setNonPreedit(true);
    return composer.nonPreedit();
}

} // namespace

TEST_CASE("the same client stays refused across a re-activation") {
    Composer composer = refusedBy("cursor");
    // The bug this fixes: Fcitx5 re-activates on a capability change, which is not a
    // new client and must not reopen a question already answered.
    CHECK_FALSE(modeAfterFocusing(composer, "cursor"));
}

TEST_CASE("a different client is a new question") {
    Composer composer = refusedBy("cursor");
    CHECK(modeAfterFocusing(composer, "gnome-text-editor"));
}

TEST_CASE("a refused client is still refused after visiting another") {
    Composer composer = refusedBy("cursor");
    REQUIRE(modeAfterFocusing(composer, "gnome-text-editor"));
    // Alternating between two apps is the ordinary way to work, so remembering only
    // the client in focus would forgive the broken one on every switch back.
    CHECK_FALSE(modeAfterFocusing(composer, "cursor"));
}

TEST_CASE("an unnamed client is always a new question") {
    Composer composer = refusedBy("");
    // The IBus shell has no name to give, and on GNOME/Wayland every client answers
    // `gnome-shell` — one name for all of them. Remembering either would stand the mode
    // down for everybody at once, so an empty name keeps the old per-focus behaviour.
    CHECK(modeAfterFocusing(composer, ""));
}

TEST_CASE("the memory is bounded") {
    Composer composer = refusedBy("client0");
    // Eight is the cap. Refusing eight more clients pushes the first one out, and it is
    // simply judged again — costing it one word, not correctness.
    for (int i = 1; i <= 8; ++i) {
        const std::string name = "client" + std::to_string(i);
        REQUIRE(modeAfterFocusing(composer, name.c_str()));
        typeUntilRefused(composer, "tieengs", applyIgnoringDeletes);
        REQUIRE_FALSE(composer.nonPreedit());
    }
    CHECK(modeAfterFocusing(composer, "client0"));
    CHECK_FALSE(modeAfterFocusing(composer, "client8"));
}
