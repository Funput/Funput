// When the gnome-shell bridge is certain to drop the next repair — the truth table
// of transport/guard.h. The pipeline behind it is exercised in sim/bridge/; this
// pins the rule itself, and that it is a rule about the transport, not about the
// client: a direct path is never guarded, whatever the reading. With it, the other
// thing the composer decides by transport: what a reset does to the composing word —
// and what it does to the journal, which is the same on every transport.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/appedits/sync.h"
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

TEST_CASE("a reset flushes on a direct transport and discards on the bridge") {
    // Fcitx5's `reset()`. Direct: nothing else commits the preedit, so the composer
    // does. gnome-shell: the reset is a click, and mutter committed the preedit before
    // forwarding it (`clutter_input_focus_reset`, COMMIT mode); a flush would type
    // the word twice. The plan still drops the preedit the shell shows.
    Composer direct = composerFor(Method::Telex);
    direct.onFocusChanged(identifyProgram("firefox"));
    type(direct, "tieengs");
    REQUIRE(direct.isComposing());
    const ComposePlan flushed = direct.onReset();
    CHECK(flushed.effect == Effect::Commit);
    CHECK(flushed.text == "tiếng");
    CHECK_FALSE(direct.isComposing());

    Composer bridged = composerFor(Method::Telex);
    bridged.onFocusChanged(identifyProgram("gnome-shell"));
    type(bridged, "tieengs");
    REQUIRE(bridged.isComposing());
    const ComposePlan dropped = bridged.onReset();
    CHECK(dropped.effect == Effect::Commit);
    CHECK(dropped.text.empty());
    CHECK_FALSE(dropped.consumed);
    CHECK_FALSE(bridged.isComposing());
}

// RFC §6.1, invariant 7: a reset is a click or a caret move, so the journal must not
// outlive it on any transport, through either entry — Fcitx5's `onReset()`, which
// flushes or drops by transport, and the IBus shell's `onResetCommitted()`. Pinned on
// the Backspace that follows: in sync it is taken over and re-opens `phủ`; after a
// reset it is the app's and re-opens nothing, since the document the journal
// described is behind the caret. The IBus shell used to discard only.
TEST_CASE("a reset forgets the journal on every transport") {
    for (const char *program : {"firefox", "gnome-shell", ""}) {
        CAPTURE(program);
        for (const bool committedByFramework : {false, true}) {
            CAPTURE(committedByFramework);
            Composer composer = composerFor(Method::Telex);
            composer.onFocusChanged(identifyProgram(program));
            composer.setNonPreedit(true);
            std::string document = typeInSync(composer, "phur ");
            REQUIRE(document == "phủ ");
            if (committedByFramework) {
                composer.onResetCommitted();
            } else {
                composer.onReset();
            }
            CHECK_FALSE(composer.onKey(bare(keysym::BackSpace)).consumed);
            popChars(document, 1);
            CHECK_FALSE(composer.adoptWordBeforeBackspace());
        }
    }
    // The control: the same keys with no reset between them.
    Composer composer = composerFor(Method::Telex);
    composer.setNonPreedit(true);
    std::string document = typeInSync(composer, "phur ");
    CHECK(composer.onKey(bare(keysym::BackSpace)).consumed);
    popChars(document, 1);
    CHECK(composer.adoptWordBeforeBackspace());
}
