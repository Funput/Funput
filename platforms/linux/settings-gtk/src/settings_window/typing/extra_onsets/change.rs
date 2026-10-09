//! What one click does to the letter set — the section's only rule, kept apart from
//! the widgets so it is tested without a display.

use crate::settings::{ExtraOnsetLetters, OnsetLetter};

/// One change the section's widgets report.
#[derive(Debug, Clone, Copy)]
pub(super) enum Click {
    /// The switch: on admits every letter (UniKey's behaviour), off admits none.
    Switch(bool),
    /// One checkbox: admit the letter or not.
    Letter(OnsetLetter, bool),
}

/// The set `click` makes of the `shown` one, or `None` when it changes nothing to
/// save — which is how the notifies the section's own redraw causes end here. The
/// switch reads as on exactly while some letter is admitted, so a switch reporting
/// that is a redraw, not a request to tick all four.
pub(super) fn next(shown: ExtraOnsetLetters, click: Click) -> Option<ExtraOnsetLetters> {
    let letters = match click {
        Click::Switch(on) if on == !shown.is_empty() => return None,
        Click::Switch(true) => ExtraOnsetLetters::ALL,
        Click::Switch(false) => ExtraOnsetLetters::NONE,
        Click::Letter(letter, on) => shown.with(letter, on),
    };
    (letters != shown).then_some(letters)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn set(id: &str) -> ExtraOnsetLetters {
        ExtraOnsetLetters::from_id(id)
    }

    #[test]
    fn the_switch_ticks_all_four_or_clears_them() {
        assert_eq!(
            next(set(""), Click::Switch(true)),
            Some(ExtraOnsetLetters::ALL)
        );
        assert_eq!(next(set("zj"), Click::Switch(false)), Some(set("")));
    }

    #[test]
    fn a_switch_that_agrees_with_the_letters_saves_nothing() {
        // Showing `z` turns the switch on; that notify must not become "all four".
        assert_eq!(next(set("z"), Click::Switch(true)), None);
        assert_eq!(next(set(""), Click::Switch(false)), None);
    }

    #[test]
    fn a_checkbox_adds_or_removes_only_its_letter() {
        assert_eq!(
            next(set("z"), Click::Letter(OnsetLetter::F, true)),
            Some(set("zf"))
        );
        assert_eq!(
            next(set("zf"), Click::Letter(OnsetLetter::Z, false)),
            Some(set("f"))
        );
    }

    #[test]
    fn unticking_the_last_letter_empties_the_set() {
        // An empty set is what turns the switch off on the next redraw.
        assert_eq!(
            next(set("w"), Click::Letter(OnsetLetter::W, false)),
            Some(set(""))
        );
    }

    #[test]
    fn a_checkbox_already_in_that_state_saves_nothing() {
        assert_eq!(next(set("z"), Click::Letter(OnsetLetter::Z, true)), None);
        assert_eq!(next(set("z"), Click::Letter(OnsetLetter::J, false)), None);
    }
}
