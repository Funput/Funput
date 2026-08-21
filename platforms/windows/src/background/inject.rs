//! Emit an [`InjectPlan`] to the focused app: Backspace presses, then Unicode
//! characters, via `SendInput`. Every synthesized event carries [`INJECT_TAG`] in
//! `dwExtraInfo` so the hook ignores them (no re-entrancy).
//!
//! Backspace is the only deletion key that ever goes out, never `Delete`, and the
//! caret is never steered with an arrow key. Everything a plan replaces sits
//! *behind* the caret, so an injection is built to be incapable of reaching what is
//! in front of it — including when the app holds a selection there, which
//! [`send_plan`] neutralizes with a lead character where that can happen at all.
//! Where that is is [`lead`]'s business, and its alone.

mod events;
mod lead;
mod modifiers;

use funput_desktop::InjectPlan;

pub use lead::note_foreground;
pub use modifiers::send_plan_unmodified;

use events::{deletions, raw_send, text};

/// The leading UTF-16 units of `units`' first character — both halves of a surrogate
/// pair, or the single unit of a BMP character. `None` for an empty plan.
///
/// Vietnamese NFC never leaves the BMP, but a gõ tắt expansion carries whatever the
/// user typed into the table, so the pair must not be split: half a surrogate is not
/// a character the app would delete with one Backspace.
fn leading_char(units: &[u16]) -> Option<&[u16]> {
    let first = *units.first()?;
    let paired = (0xD800..=0xDBFF).contains(&first) && units.len() >= 2;
    Some(&units[..if paired { 2 } else { 1 }])
}

/// Send a plan as one atomic `SendInput` batch. The only injection route there is.
///
/// # The ambiguous first Backspace
///
/// A plan says "delete the `backspaces` characters behind the caret, then type
/// `units`". Backspace cannot express that on its own, because it means two
/// different things: with a selection live it deletes the **selection**, otherwise a
/// character. Apps put a selection in front of the caret without being asked —
/// Chrome's omnibox and Firefox's address bar inline-autofill a selected suffix, so
/// "exa" displays as "exa[mple.com]". The first Backspace then spends itself on that
/// suffix, one stale character survives, and the new glyph piles on top: "go" + `w`
/// gives "goơ" instead of "gơ".
///
/// A hook shell cannot read the document, so it cannot look for the selection, and
/// Windows will not say where the caret is: Chrome draws its whole UI in one HWND,
/// so `GetGUIThreadInfo` reports that same top-level window for the omnibox and for
/// a page field alike, with no caret to inspect. Only UI Automation separates them,
/// which a `WH_KEYBOARD_LL` callback cannot afford. An earlier fix guessed instead —
/// a `Delete` primer for a hard-coded list of browsers — and guessed wrong in every
/// page field of those browsers, eating the character to the right of the caret on
/// every keystroke typed into the middle of a line.
///
/// # Normalizing instead of guessing
///
/// There is one keystroke that does not care: **typing a character**. A selection in
/// front of the caret is replaced by it; no selection and it is simply inserted.
/// Both land on the same state, so leading with one erases the difference:
///
/// ```text
///   selection live:  [keep][stale]|[sel][rest]  --type S[0]-->  [keep][stale][S0]|[rest]
///   no selection:    [keep][stale]|[rest]       --type S[0]-->  [keep][stale][S0]|[rest]
/// ```
///
/// From there `backspaces + 1` Backspaces bite `S[0]` and the stale characters, and
/// the plan's text goes down on clean ground. The lead is the first character of
/// that text, so a field that accepts the result accepts it too, and no arrow key is
/// needed to get back. A pure insert (`backspaces == 0`) skips all this: its text is
/// already the one keystroke that neutralizes a selection.
///
/// # Why the lead is not paid everywhere
///
/// That extra Backspace is the whole cost of the trick, and some apps cannot pay it:
/// a text engine that drops a key arriving in the same burst as the glyph before it
/// loses precisely the lead's own Backspace and lands one character short. CorelDRAW
/// does exactly that. So the lead is spent only where a selection can actually turn
/// up in front of the caret, which [`lead`] decides by asking which engine drew the
/// window rather than which app it is — and where being wrong is free in both
/// directions, unlike the `Delete` primer.
///
/// # What it does not cover
///
/// The lead only holds if it leaves the app with nothing to re-select, and a batch
/// is not atomic against that. Measured in Chrome's omnibox: given "exa[mple.com]",
/// a lead that *keeps* the URL matching brings the autofill straight back between
/// the keystrokes of one `SendInput`, and the Backspaces are ambiguous again.
///
/// Reaching that needs `units[0]` to be the character the URL continues with, which
/// a plan does not normally produce: `units[0]` *replaces* a character already on
/// screen, so leading with it appends a letter the text just had rather than the one
/// the match wants. A tone or shape leads with a Vietnamese glyph ("exa" + `à`
/// measured correct); an English restore re-types the composing word's own first
/// letter. Only a coincidence — a gõ tắt expansion whose first letter happens to be
/// the URL's next one — lines up, and it costs one visibly wrong syllable in an
/// address bar, not lost text.
///
/// The other cost: in a field sitting at its exact `maxlength` the lead is refused,
/// and the extra Backspace then takes a real character. Rare, immediately visible,
/// and confined to a field the user cannot type into anyway.
pub fn send_plan(plan: &InjectPlan) {
    if plan.is_noop() {
        return;
    }
    let lead = (plan.backspaces > 0 && lead::wanted())
        .then(|| leading_char(&plan.units))
        .flatten();
    let mut inputs = Vec::new();
    if let Some(lead) = lead {
        inputs.extend(text(lead));
        inputs.extend(deletions(plan.backspaces + 1));
    } else {
        inputs.extend(deletions(plan.backspaces));
    }
    inputs.extend(text(&plan.units));
    raw_send(&inputs);
}

#[cfg(test)]
mod tests {
    use windows::Win32::UI::Input::KeyboardAndMouse::{KEYEVENTF_KEYUP, VK_BACK, VK_DELETE};

    use super::events::VK_BACK_SCAN;
    use super::*;

    /// The batch [`send_plan`] would emit, as `(wVk, wScan, keyup)` per event.
    /// Mirrors its assembly rather than calling it, since `send_plan` ends in
    /// `SendInput` and would type into whatever window the test runner has focused.
    /// `leading` stands in for [`lead::wanted`], which the real thing reads off the
    /// foreground app.
    fn batch(backspaces: usize, output: &str, leading: bool) -> Vec<(u16, u16, bool)> {
        let units: Vec<u16> = output.encode_utf16().collect();
        let lead = (backspaces > 0 && leading)
            .then(|| leading_char(&units))
            .flatten();
        let mut inputs = Vec::new();
        match lead {
            Some(lead) => {
                inputs.extend(text(lead));
                inputs.extend(deletions(backspaces + 1));
            }
            None => inputs.extend(deletions(backspaces)),
        }
        inputs.extend(text(&units));
        inputs
            .iter()
            .map(|i| {
                let ki = unsafe { i.Anonymous.ki };
                (ki.wVk.0, ki.wScan, (ki.dwFlags.0 & KEYEVENTF_KEYUP.0) != 0)
            })
            .collect()
    }

    /// How many characters the batch types, and how many it deletes.
    fn typed_and_deleted(emitted: &[(u16, u16, bool)]) -> (usize, usize) {
        let down = emitted.iter().filter(|&&(.., up)| !up);
        let (mut typed, mut deleted) = (0, 0);
        for &(vk, ..) in down {
            if vk == VK_BACK.0 {
                deleted += 1;
            } else {
                typed += 1;
            }
        }
        (typed, deleted)
    }

    #[test]
    fn a_replacement_leads_with_one_character_and_pays_for_it() {
        // "bo" + `j` -> "bộ": type the lead, delete it plus the stale vowel, then
        // lay down the text. The lead is what makes the deletions unambiguous even
        // when the app holds a selection in front of the caret.
        assert_eq!(
            batch(1, "ộ", true),
            vec![
                (0, 'ộ' as u16, false),
                (0, 'ộ' as u16, true),
                (VK_BACK.0, VK_BACK_SCAN, false),
                (VK_BACK.0, VK_BACK_SCAN, true),
                (VK_BACK.0, VK_BACK_SCAN, false),
                (VK_BACK.0, VK_BACK_SCAN, true),
                (0, 'ộ' as u16, false),
                (0, 'ộ' as u16, true),
            ]
        );
    }

    #[test]
    fn the_lead_is_always_paid_back() {
        // Whatever the plan, the batch deletes exactly the characters it typed on
        // top of the ones the plan asked for — never a character more.
        for (backspaces, output) in [(1, "ộ"), (3, "card "), (7, "Việt Nam ")] {
            let (typed, deleted) = typed_and_deleted(&batch(backspaces, output, true));
            assert_eq!(deleted, backspaces + 1, "plan ({backspaces}, {output:?})");
            assert_eq!(typed, output.chars().count() + 1);
        }
    }

    #[test]
    fn without_the_lead_a_plan_is_sent_exactly_as_written() {
        // The regression CorelDRAW reported: its text tool drops the Backspace that
        // arrives right behind an inserted glyph, so the lead's own Backspace never
        // lands and "dương" (4 Backspaces + "ương") came out "duương". No lead, no
        // extra Backspace, nothing for it to lose.
        assert_eq!(
            batch(4, "ương", false),
            [
                [
                    (VK_BACK.0, VK_BACK_SCAN, false),
                    (VK_BACK.0, VK_BACK_SCAN, true)
                ]
                .repeat(4),
                "ương"
                    .encode_utf16()
                    .flat_map(|u| [(0, u, false), (0, u, true)])
                    .collect(),
            ]
            .concat()
        );
        let (typed, deleted) = typed_and_deleted(&batch(4, "ương", false));
        assert_eq!((typed, deleted), ("ương".chars().count(), 4));
    }

    #[test]
    fn a_pure_insert_needs_no_lead() {
        // Its own text is already the keystroke that neutralizes a selection.
        for leading in [true, false] {
            assert_eq!(
                batch(0, "à", leading),
                vec![(0, 'à' as u16, false), (0, 'à' as u16, true)]
            );
        }
    }

    #[test]
    fn a_surrogate_pair_leads_with_both_halves() {
        // One character, two units: splitting it would type half a code point and
        // then spend one Backspace on something the app never rendered.
        let emitted = batch(1, "😀x", true);
        let units: Vec<u16> = "😀".encode_utf16().collect();
        assert_eq!(units.len(), 2);
        assert_eq!(
            &emitted[..4],
            &[
                (0, units[0], false),
                (0, units[0], true),
                (0, units[1], false),
                (0, units[1], true),
            ]
        );
        assert_eq!(typed_and_deleted(&emitted).1, 2); // one lead char, one stale
    }

    #[test]
    fn nothing_deletes_forward() {
        // The regression this file guards: a plan only ever describes characters
        // *behind* the caret, so no batch may carry a key that removes what is in
        // front of it. A `Delete` primer here once cost one character of existing
        // text per keystroke typed into the middle of a line — see [`send_plan`].
        for (backspaces, output) in [(0, "à"), (1, "ộ"), (3, "card ")] {
            for leading in [true, false] {
                assert!(
                    batch(backspaces, output, leading)
                        .iter()
                        .all(|&(vk, ..)| vk != VK_DELETE.0),
                    "forward delete in plan ({backspaces}, {output:?})"
                );
            }
        }
    }
}
