//! The Kiểu chữ bar: five ways to re-case whatever the window is converting.
//!
//! The Chuyển mã window's second axis, and a bar rather than a tab because the two axes
//! **compose** — a paragraph read out of TCVN3 can still be title-cased — so both
//! choices belong on screen at once. A tab would say the user has to pick one.
//! `docs/features/text-case.md` is where that decision lives.
//!
//! **Two instances, one session.** Slint uses a single `ConvertCasingBar` component in
//! both shapes because its state arrives through a global; a `GtkWidget` has exactly one
//! parent, so the text pane and the batch pane each own a [`Bar`]. They cannot disagree:
//! the hidden pane is never refreshed, which is already how the two target dropdowns
//! work.
//!
//! Nothing here decides what a transform *does*, or what it is called —
//! `funput_convert::casing` owns the menu, its order, and the Vietnamese names, so the
//! three shells cannot drift. What this file decides is presentation: which chip is lit,
//! what the `Đang áp` line reads, which switch is on screen, and whether the bar is live
//! at all. Those four are pure functions of the view, and the only thing here with
//! tests.

mod applied;
mod chips;

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::Convert;
use funput_convert::{Mode, View, casing};
use funput_core::textcase::Transform;

pub(in crate::convert) struct Bar {
    pub(in crate::convert) root: gtk::Box,
    chips: chips::Chips,
    applied: applied::Applied,
}

impl Bar {
    pub(in crate::convert) fn new() -> Self {
        let chips = chips::Chips::new();
        let applied = applied::Applied::new();

        let root = gtk::Box::builder()
            .orientation(gtk::Orientation::Vertical)
            .spacing(6)
            .build();
        root.append(&chips.root);
        root.append(&applied.root);

        Self {
            root,
            chips,
            applied,
        }
    }

    pub(in crate::convert) fn wire(&self, convert: &Rc<Convert>) {
        self.chips.wire(convert);
        self.applied.wire(convert);
    }

    /// Takes no `&Rc<Convert>`, unlike the footer: nothing on this bar reads `is_busy`.
    /// A batch already running cannot be changed by a press — `Session::batch_job`
    /// snapshots the casing before the work leaves the thread — so leaving the bar live
    /// during a run costs only that the `N chữ sẽ mất` column re-costs against a
    /// transform the running job does not have. Windows makes the same call; macOS dims
    /// the bar instead, and that difference is noted in the feature document.
    pub(in crate::convert) fn refresh(&self, view: &View) {
        // One call, not a hand-written dim as well: GTK fades an insensitive subtree
        // itself, so there is nothing here to match the `opacity: 0.45` the Slint bar
        // has to spell out.
        self.root.set_sensitive(usable(view));
        self.chips.refresh(&lit(view));
        self.applied.refresh(view, &applied_line(view));
    }
}

/// Which chips are lit — one bool per entry of `casing::ALL`, in menu order.
fn lit(view: &View) -> Vec<bool> {
    (0..casing::ALL.len())
        .map(|index| view.transforms.contains(&index))
        .collect()
}

/// The `Đang áp` line, empty when nothing is applied.
///
/// Spells the order out because the order changes the text: lowercase then title case
/// is not title case then lowercase.
fn applied_line(view: &View) -> String {
    if view.transforms.is_empty() {
        return String::new();
    }
    let names: Vec<&str> = view
        .transforms
        .iter()
        .map(|&index| casing::name(casing::at(index)))
        .collect();
    format!("Đang áp: {}", names.join(" → "))
}

/// Whether a transform is applied, asked **by name** — so a switch's owner is never
/// written down as a position in the menu, which is append-only and not ours to count.
fn applies(view: &View, transform: Transform) -> bool {
    casing::index_of(transform).is_some_and(|index| view.transforms.contains(&index))
}

/// Text nothing can read cannot be converted, and cannot be re-cased either: one rule,
/// not two. A batch carries a charset per row, so it is never blocked.
fn usable(view: &View) -> bool {
    view.mode == Mode::Files || view.source.is_some()
}

#[cfg(test)]
mod tests {
    use super::*;
    use funput_convert::Session;

    /// A settled document with some transforms pressed, in the order given.
    ///
    /// Drives a real `Session` because `View` is `#[non_exhaustive]` — the same reason
    /// the Windows tests do, and the reason these assertions are worth having: they
    /// check this window against the crate rather than against a fixture of its own.
    fn viewed(pressed: &[Transform]) -> Session {
        let mut session = Session::new();
        session.set_input("Tiếng Việt rất đẹp".to_string());
        for &transform in pressed {
            session.apply_transform(casing::index_of(transform).expect("in the menu"));
        }
        session.refresh();
        session
    }

    #[test]
    fn the_menu_is_whole_before_anything_is_pressed() {
        // Load-bearing here in a way it is not in Slint: the chips are built from
        // `casing::ALL` and refreshed by zipping a slice of bools, so a length that
        // drifted would quietly stop lighting the last chip rather than fail.
        let session = viewed(&[]);
        let lit = lit(session.view());
        assert_eq!(lit.len(), casing::ALL.len());
        assert!(lit.iter().all(|on| !on));
    }

    #[test]
    fn a_pressed_transform_lights_its_own_chip() {
        let session = viewed(&[Transform::Lower]);
        let lower = casing::index_of(Transform::Lower).expect("in the menu");
        for (index, on) in lit(session.view()).into_iter().enumerate() {
            assert_eq!(on, index == lower, "chip {index}");
        }
    }

    #[test]
    fn the_applied_line_is_empty_until_something_is_pressed() {
        assert_eq!(applied_line(viewed(&[]).view()), "");
    }

    #[test]
    fn the_applied_line_reads_in_the_order_pressed() {
        let session = viewed(&[Transform::Lower, Transform::Title]);
        assert_eq!(
            applied_line(session.view()),
            "Đang áp: chữ thường → Viết Hoa Đầu Mỗi Từ"
        );
        let session = viewed(&[Transform::Title, Transform::Lower]);
        assert_eq!(
            applied_line(session.view()),
            "Đang áp: Viết Hoa Đầu Mỗi Từ → chữ thường"
        );
    }

    #[test]
    fn a_switch_shows_only_for_the_transform_that_owns_it() {
        let session = viewed(&[Transform::NoDiacritics]);
        assert!(applies(session.view(), Transform::NoDiacritics));
        assert!(!applies(session.view(), Transform::Title));
    }

    #[test]
    fn typing_a_new_document_takes_the_transforms_off() {
        let mut session = viewed(&[Transform::Upper]);
        session.set_input("Một đoạn khác".to_string());
        session.refresh();
        assert_eq!(applied_line(session.view()), "");
        assert!(lit(session.view()).iter().all(|on| !on));
    }

    #[test]
    fn the_bar_is_dead_until_a_charset_explains_the_document() {
        // The batch arm is not reachable without files on disk; `Mode::Files` carrying
        // a charset per row is the crate's invariant and is tested there.
        let mut session = Session::new();
        session.set_input("hello world".to_string());
        session.refresh();
        assert!(!usable(session.view()));
        session.set_input("Tiếng Việt".to_string());
        session.refresh();
        assert!(usable(session.view()));
    }
}
