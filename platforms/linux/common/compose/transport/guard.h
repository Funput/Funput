// When the gnome-shell bridge is certain to drop the next repair.
//
// Non-preedit judges a client only after a write (nonpreedit.h): silence is not a
// verdict, and an input gate on the content of surrounding text was tried and
// reverted because it kept the mode off in Chrome (README Linux). This is not that
// gate. It accuses nobody and remembers nothing. It covers the one case where the
// outcome of a delete is known *before* it is issued, from the code of the hops it
// crosses rather than from anything the client did, and it is consulted only between
// words, where the mode may change (RFC #640 §6.1, invariant 5).
//
// On GNOME/Wayland a `deleteSurroundingText` is not delivered to the app. It is
// converted twice on cached copies of the surrounding text (RFC §4.1, §4.3):
//
//   gnome-shell  js/misc/inputMethod.js  `_onDeleteSurroundingText`
//       if (this._surroundingText === null) { log(...); return; }
//   mutter       meta-wayland-text-input.c  `..._focus_delete_surrounding`
//       before = g_utf8_offset_to_pointer (cursor, offset);   // chars -> bytes
//       g_return_if_fail (before >= start);                   // warns and returns
//
// Both drop the delete and nothing else: the commit that follows still lands, so
// `[[` leaves `ơ[` and `soor` leaves `sôổ`. Neither failure throws, so the retry in
// gnome-shell's `catch` never runs, and the engine is told nothing. Two states of the
// bridge make the drop certain:
//
//   1. The client has not sent surrounding text since the last focus or reset.
//      gnome-shell's `vfunc_reset` — mutter calls it on a click into the focused
//      surface — nulls `_surroundingText`, and it stays null until the client's next
//      answer. A focus starts the same way. Every delete in that window is dropped,
//      whatever the reading the shell still holds from before.
//   2. The client's last answer had nothing before the caret: the start of a field.
//      mutter's copy is that same answer, and `g_utf8_offset_to_pointer` walked back
//      N characters from the start of an empty prefix fails `before >= start` for
//      any N ≥ 1 — which is what every repair after the first keystroke issues.
//
// A non-empty prefix of L characters is not guarded, even if the client is a
// keystroke late: a repair deletes within the word being typed, which is shorter than
// any prefix long enough to hold a word, so N ≤ L and the conversion is an identity
// (RFC §4.5). The per-keystroke test "reading shorter than deleteChars" is not taken
// either: it can only be evaluated mid-word, where the mode must not change.
//
// Only the gnome-shell transport. Everywhere else the delete reaches the app as
// issued, and a stale reading proves nothing about what it will do — that is the
// verdict's job, after the write.

#ifndef FUNPUT_COMPOSE_TRANSPORT_GUARD_H
#define FUNPUT_COMPOSE_TRANSPORT_GUARD_H

#include <string>

#include "compose/transport.h"

namespace funput {

// `spokeSinceReset`: whether surrounding text arrived since the last focus-in or
// reset, as the shell counts it. `textBeforeCaret`: the client's last answer.
inline bool bridgeBlocksRepairs(Transport transport, bool spokeSinceReset,
                                const std::string &textBeforeCaret) {
    switch (transport) {
    case Transport::Direct:
        return false;
    case Transport::GnomeShell:
        return !spokeSinceReset || textBeforeCaret.empty();
    }
    return false;
}

} // namespace funput

#endif // FUNPUT_COMPOSE_TRANSPORT_GUARD_H
