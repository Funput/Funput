// When the gnome-shell bridge is certain to drop the next repair — the truth table
// of transport/guard.h. The pipeline behind it is exercised in sim/bridge_tests.cpp;
// this pins the rule itself, and that it is a rule about the transport, not about
// the client: a direct path is never guarded, whatever the reading.

#include <doctest/doctest.h>

#include "compose/transport/guard.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

constexpr bool kSpoke = true;

TEST_CASE("a direct transport is never guarded") {
    // Both conditions true, and still not: a stale or empty reading over a direct
    // path proves nothing about the delete, and judging it before the write is the
    // input gate the README records as tried and reverted.
    CHECK_FALSE(bridgeBlocksRepairs(Transport::Direct, !kSpoke, ""));
    CHECK_FALSE(bridgeBlocksRepairs(Transport::Direct, kSpoke, ""));
    CHECK_FALSE(bridgeBlocksRepairs(Transport::Direct, !kSpoke, "abc "));
    CHECK_FALSE(bridgeBlocksRepairs(Transport::Direct, kSpoke, "abc "));
}

TEST_CASE("the bridge has nothing to convert a delete on until the client answers") {
    // gnome-shell's `_surroundingText` is null after a focus or a reset, whatever the
    // shell still holds from before.
    CHECK(bridgeBlocksRepairs(Transport::GnomeShell, !kSpoke, ""));
    CHECK(bridgeBlocksRepairs(Transport::GnomeShell, !kSpoke, "abc "));
}

TEST_CASE("the bridge cannot walk back from the start of an empty prefix") {
    // mutter's `g_utf8_offset_to_pointer (cursor, -N)` on an empty copy fails
    // `before >= start` for every N a repair issues.
    CHECK(bridgeBlocksRepairs(Transport::GnomeShell, kSpoke, ""));
}

TEST_CASE("a non-empty prefix carries a repair") {
    // A repair deletes within the word being typed, shorter than any prefix long
    // enough to hold the word before it; the conversion is then an identity.
    CHECK_FALSE(bridgeBlocksRepairs(Transport::GnomeShell, kSpoke, "abc "));
    CHECK_FALSE(bridgeBlocksRepairs(Transport::GnomeShell, kSpoke, "ế"));
}

TEST_CASE("the composer answers for the transport it was told at focus") {
    Composer composer = composerFor(Method::Telex);
    CHECK_FALSE(composer.bridgeBlocksRepairs(kSpoke, ""));

    composer.onFocusChanged(identifyProgram("gnome-shell"));
    CHECK(composer.bridgeBlocksRepairs(kSpoke, ""));
    CHECK(composer.bridgeBlocksRepairs(!kSpoke, "abc "));
    CHECK_FALSE(composer.bridgeBlocksRepairs(kSpoke, "abc "));

    // A direct client after the bridge: the transport is per focus, remembered no
    // longer than the focus it was told in.
    composer.onFocusChanged(identifyProgram("firefox"));
    CHECK_FALSE(composer.bridgeBlocksRepairs(kSpoke, ""));
    composer.onFocusChanged();
    CHECK_FALSE(composer.bridgeBlocksRepairs(!kSpoke, ""));
}
