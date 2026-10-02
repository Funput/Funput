//! Settings → Cách gõ → "Phụ âm đầu mở rộng": the `ExtraOnsetsState` global and
//! the shell state behind it.
//!
//! Rust is the source of truth. Every click writes the new letter set through
//! `commands`, then [`show`] pushes it back — which is how unticking the last
//! letter turns the switch off without the UI keeping rules of its own.

use slint::{ComponentHandle, ModelRc, VecModel};

use crate::shared::{commands, shell};
use crate::{ExtraOnsetsState, OnsetChoice, SettingsWindow};
use funput_config::{ExtraOnsetLetters, OnsetLetter};

pub(super) fn wire(window: &SettingsWindow) {
    let state = window.global::<ExtraOnsetsState>();

    let weak = window.as_weak();
    state.on_set_all(move |on| {
        let letters = if on {
            ExtraOnsetLetters::ALL
        } else {
            ExtraOnsetLetters::NONE
        };
        apply(&weak, letters);
    });

    let weak = window.as_weak();
    state.on_set_letter(move |symbol, on| {
        let Some(letter) = symbol.chars().next().and_then(OnsetLetter::from_symbol) else {
            return;
        };
        apply(&weak, shell::snapshot().extra_onsets.with(letter, on));
    });
}

/// Show `letters` in the section: the switch, every checkbox, and the hint flag.
pub(in crate::ui) fn show(window: &SettingsWindow, letters: ExtraOnsetLetters) {
    let state = window.global::<ExtraOnsetsState>();
    let choices: Vec<OnsetChoice> = OnsetLetter::ALL
        .into_iter()
        .map(|letter| OnsetChoice {
            letter: letter.symbol().to_string().into(),
            examples: letter.examples().into(),
            on: letters.contains(letter),
        })
        .collect();
    state.set_letters(ModelRc::new(VecModel::from(choices)));
    state.set_enabled(!letters.is_empty());
    state.set_w_admitted(letters.contains(OnsetLetter::W));
}

fn apply(weak: &slint::Weak<SettingsWindow>, letters: ExtraOnsetLetters) {
    commands::set_extra_onsets(letters);
    if let Some(window) = weak.upgrade() {
        show(&window, letters);
    }
}
