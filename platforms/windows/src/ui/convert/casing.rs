//! The second axis: this shell's half of it.
//!
//! Nothing here decides anything either. Which transforms exist, what each is
//! called, what pressing one does to a document and what a second press on the same
//! one means — all of it lives in [`funput_convert::casing`], shared with the GTK
//! window on Linux and with macOS through the C door. What is here is the mapping:
//! a [`View`] poured into the `Casing` global, and five buttons tied back to the
//! session.
//!
//! The one thing this file must not do is write a transform's position down as a
//! number. A switch belongs to a transform, not to a slot, so it asks
//! [`funput_convert::casing::index_of`] where that transform sits — appending a
//! sixth transform in core then moves nothing here.

use funput_convert::{Mode, Session, View, casing};
use funput_core::textcase::Transform;
use slint::{ComponentHandle, ModelRc, VecModel};

use crate::{Casing, ConvertWindow, TransformChip};

use super::view::edit;

pub(super) fn wire(window: &ConvertWindow) {
    let casing = window.global::<Casing>();
    casing.on_apply(|index| edit(move |s| s.apply_transform(usize::try_from(index).unwrap_or(0))));
    casing.on_undo(|| edit(Session::undo_transform));
    casing.on_clear(|| edit(Session::clear_transforms));
    casing.on_set_keep_d(|on| edit(move |s| s.set_keep_d(on)));
    casing.on_set_flatten_caps(|on| edit(move |s| s.set_flatten_caps(on)));
}

pub(super) fn show(window: &ConvertWindow, view: &View) {
    let casing = window.global::<Casing>();
    casing.set_transforms(ModelRc::new(VecModel::from(chips(view))));
    casing.set_applied_line(applied_line(view).into());
    // A document no charset could explain is not converted, and core does not
    // transform it either — one rule, not two. A batch carries a charset per row, so
    // it is never blocked.
    casing.set_enabled(view.mode == Mode::Files || view.source.is_some());
    casing.set_keep_d(view.keep_d);
    casing.set_flatten_caps(view.flatten_caps);
    casing.set_shows_keep_d(applies(view, Transform::NoDiacritics));
    casing.set_shows_flatten_caps(applies(view, Transform::Title));
}

/// The menu, and which of it is on.
fn chips(view: &View) -> Vec<TransformChip> {
    casing::ALL
        .iter()
        .enumerate()
        .map(|(index, &transform)| TransformChip {
            name: casing::name(transform).into(),
            applied: view.transforms.contains(&index),
        })
        .collect()
}

/// What is applied, in the order pressed.
///
/// The order is the whole point — `chữ thường → Viết Hoa Đầu Mỗi Từ` is a different
/// document from the same two reversed — so it is spelled out rather than left for
/// the user to infer from which chips are lit. Empty when nothing is applied, which
/// is what hides the row.
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

/// Whether the transform that owns a switch is applied.
fn applies(view: &View, transform: Transform) -> bool {
    casing::index_of(transform).is_some_and(|index| view.transforms.contains(&index))
}

#[cfg(test)]
mod tests {
    use super::*;

    /// `View` is `#[non_exhaustive]`, so it cannot be built field by field from out
    /// here. Driving a real session is the honest way anyway: it locks in what the
    /// window will actually be handed.
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
        let session = viewed(&[]);
        let chips = chips(session.view());
        assert_eq!(chips.len(), casing::ALL.len());
        assert!(chips.iter().all(|chip| !chip.applied));
        assert_eq!(chips[0].name, casing::name(Transform::Upper));
    }

    #[test]
    fn a_pressed_transform_lights_its_own_chip() {
        let session = viewed(&[Transform::Lower]);
        let chips = chips(session.view());
        let lit: Vec<&str> = chips
            .iter()
            .filter(|chip| chip.applied)
            .map(|chip| chip.name.as_str())
            .collect();
        assert_eq!(lit, vec![casing::name(Transform::Lower)]);
    }

    #[test]
    fn the_applied_line_is_empty_until_something_is_pressed() {
        assert_eq!(applied_line(viewed(&[]).view()), "");
    }

    #[test]
    fn the_applied_line_reads_in_the_order_pressed() {
        let forward = viewed(&[Transform::Lower, Transform::Title]);
        let backward = viewed(&[Transform::Title, Transform::Lower]);
        assert_eq!(
            applied_line(forward.view()),
            "Đang áp: chữ thường → Viết Hoa Đầu Mỗi Từ"
        );
        assert_eq!(
            applied_line(backward.view()),
            "Đang áp: Viết Hoa Đầu Mỗi Từ → chữ thường"
        );
    }

    #[test]
    fn a_switch_shows_only_for_the_transform_that_owns_it() {
        let session = viewed(&[Transform::NoDiacritics]);
        assert!(applies(session.view(), Transform::NoDiacritics));
        assert!(!applies(session.view(), Transform::Title));
    }

    /// The bar's state comes from the session, so the window empties itself when
    /// core drops the presses — it has no list of its own to forget to clear.
    #[test]
    fn typing_a_new_document_takes_the_transforms_off() {
        let mut session = viewed(&[Transform::Upper]);
        session.set_input("xin chào".to_string());
        session.refresh();
        assert_eq!(applied_line(session.view()), "");
        assert!(chips(session.view()).iter().all(|chip| !chip.applied));
    }
}
