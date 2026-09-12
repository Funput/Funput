//! The second axis: what the window does to a document besides re-spelling it.
//!
//! Changing a charset and changing the case are two independent choices about one
//! document, and a window offers both at once. Keeping them in one pipeline is what
//! makes them agree — see [`render`], which is the only place either happens.
//!
//! The transforms themselves are `funput_core::textcase`'s. What is here is what a
//! *window* needs on top of them: an order to show them in, a name for each, and a
//! record of which ones the user has pressed so far.
//!
//! **Why the names are here rather than in core.** They are interface text, in the
//! same language as the loss warning and the `N chữ sẽ mất` note next door. Core
//! spells `Charset::name` because a charset's name is its name in any language; a
//! button's label is not that. `funput case` never needs one — clap writes English
//! help — so core would carry Vietnamese for a single caller.

use funput_core::charset::{self, Charset, Rendered};
use funput_core::textcase::{Options, Transform, apply};

/// The five transforms, in the order a window offers them.
///
/// Ordered like `charset::ALL` and used the same way: a shell builds its menu from
/// this and hands back a position. Append-only, for the same reason — a stored
/// index has to keep meaning the same thing across releases.
pub const ALL: [Transform; 5] = [
    Transform::Upper,
    Transform::Lower,
    Transform::NoDiacritics,
    Transform::Sentence,
    Transform::Title,
];

/// What to call each transform, in the language of the window.
///
/// `Transform` is exhaustive on purpose, so a sixth one makes this a compile error
/// rather than a button with no label.
pub fn name(transform: Transform) -> &'static str {
    match transform {
        Transform::Upper => "CHỮ HOA",
        Transform::Lower => "chữ thường",
        Transform::NoDiacritics => "Bỏ dấu tiếng Việt",
        Transform::Sentence => "Viết hoa đầu câu",
        Transform::Title => "Viết Hoa Đầu Mỗi Từ",
    }
}

/// The transform a menu position names, clamped — the rule [`crate::at`] follows for
/// charsets, and for the same reason: an index can only come from a menu built out
/// of [`ALL`], so clamping keeps a future mistake a wrong entry rather than a panic.
pub fn at(index: usize) -> Transform {
    ALL[index.min(ALL.len() - 1)]
}

/// A menu position that exists.
pub fn index_of(transform: Transform) -> Option<usize> {
    ALL.iter().position(|&t| t == transform)
}

/// The transforms a document is under, in the order they were pressed.
///
/// **A list rather than an edited document.** The window lets a user press one
/// transform after another, and undo the last one; replaying a list from the
/// original text is what makes that possible without ever writing back into the
/// paragraph the user pasted — which would send their caret home, and which GTK
/// reads as a fresh paste that forgets the charset it had detected.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Casing {
    transforms: Vec<Transform>,
    options: Options,
}

impl Casing {
    /// Press a transform.
    ///
    /// Pressing the one already on top does nothing: every transform is idempotent
    /// — core has a property test saying so — so a second press cannot change the
    /// document, and letting it grow the list would only make undo feel broken.
    pub fn push(&mut self, transform: Transform) {
        if self.transforms.last() != Some(&transform) {
            self.transforms.push(transform);
        }
    }

    /// Undo the last transform. Does nothing when there is none.
    pub fn undo(&mut self) {
        self.transforms.pop();
    }

    /// Back to the document as it arrived.
    pub fn clear(&mut self) {
        self.transforms.clear();
    }

    pub fn options(&self) -> Options {
        self.options
    }

    pub fn set_options(&mut self, options: Options) {
        self.options = options;
    }

    /// The list as menu positions, for a shell to show what is applied.
    pub(crate) fn indices(&self) -> Vec<usize> {
        self.transforms
            .iter()
            .copied()
            .filter_map(index_of)
            .collect()
    }

    fn run(&self, text: &str) -> String {
        self.transforms
            .iter()
            .fold(text.to_string(), |text, &transform| {
                apply(&text, transform, self.options)
            })
    }
}

/// Read a document, change its case, and write it out as `to` spells it.
///
/// **The only place a conversion happens in this crate.** Four callers need the same
/// answer — the preview pane, the clipboard, the bytes a file receives, and the
/// `N chữ sẽ mất` note on a batch row — and the note is the one that proves why:
/// UPPERCASE makes letters TCVN3 has no room for, so a note counted before the
/// transform would promise a cost the file will not keep to.
pub(crate) fn render(text: &str, from: Charset, casing: &Casing, to: Charset) -> Rendered {
    let mut pivoted = charset::read(text, from);
    if !casing.transforms.is_empty() {
        // Assigned rather than rebuilt: `Pivoted` is `#[non_exhaustive]`, and the two
        // counters beside the text describe the *reading* — how much of the source
        // charset was undefined, how much it respelled — which changing the case
        // neither adds to nor excuses.
        pivoted.text = casing.run(&pivoted.text);
    }
    charset::render(&pivoted, to)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A shell stores a menu position, so every position has to mean one transform
    /// and keep meaning it. This is `charset::ALL`'s contract, in the same shape.
    #[test]
    fn every_position_names_one_transform_and_survives_a_round_trip() {
        for (index, transform) in ALL.into_iter().enumerate() {
            assert_eq!(at(index), transform);
            assert_eq!(index_of(transform), Some(index));
            assert!(!name(transform).is_empty());
        }
    }

    /// Out of range is a wrong entry, never a panic — a stored index can outlive the
    /// menu it came from.
    #[test]
    fn a_position_from_a_longer_menu_is_clamped() {
        assert_eq!(at(usize::MAX), ALL[ALL.len() - 1]);
    }

    /// The list is what the user pressed, and pressing the same button twice is not
    /// two presses worth of document.
    #[test]
    fn the_list_records_presses_in_order_and_ignores_a_repeat() {
        let mut casing = Casing::default();
        casing.push(Transform::Lower);
        casing.push(Transform::Lower);
        casing.push(Transform::Title);
        assert_eq!(
            casing.indices(),
            vec![
                index_of(Transform::Lower).unwrap(),
                index_of(Transform::Title).unwrap()
            ]
        );

        casing.undo();
        assert_eq!(casing.indices(), vec![index_of(Transform::Lower).unwrap()]);
        casing.clear();
        assert!(casing.indices().is_empty());
    }

    /// Reading counts describe the read, so they have to survive the transform —
    /// a document that was half-undefined does not become clean by being uppercased.
    #[test]
    fn the_reading_counts_are_not_disturbed_by_a_transform() {
        let mut casing = Casing::default();
        casing.push(Transform::Upper);
        let plain = render(
            "việt nam",
            Charset::Unicode,
            &Casing::default(),
            Charset::Unicode,
        );
        let cased = render("việt nam", Charset::Unicode, &casing, Charset::Unicode);

        assert_eq!(cased.text, "VIỆT NAM");
        assert_eq!(cased.cost.undefined, plain.cost.undefined);
        assert_eq!(cased.cost.normalized, plain.cost.normalized);
    }
}
