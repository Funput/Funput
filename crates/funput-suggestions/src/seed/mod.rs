//! Shipped Vietnamese data for a bar the user has not taught anything yet.
//!
//! - `pairs` — the hand-written pair list Predict falls back on, and its table.
//! - `predict` — where that list meets the user's own pairs.
//! - `respell` — moving a tone mark between the two tone styles.
//!
//! Off until a shell says which language the user is typing and how they place
//! tones: Vietnamese pairs after an English word would be noise, and `hoà` is a
//! misspelling to someone who chose `hòa`. A shell that never calls
//! [`SuggestionEngine::set_seed_language`] gets exactly the bar it had before.
//!
//! See `docs/features/vietnamese-seed-suggestion.md`.

mod pairs;
mod predict;
mod respell;

use funput_core::ToneStyle;

use crate::engine::SuggestionEngine;
use pairs::PairTable;

/// What the shipped Vietnamese data may say.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub enum SeedLanguage {
    /// Nothing: the user is typing English, or the shell has not said.
    #[default]
    Off,
    /// The user is typing Vietnamese with tones placed in this style.
    Vietnamese(ToneStyle),
}

/// What an engine holds of the shipped Vietnamese data.
#[derive(Default)]
pub(crate) struct SeedSlot {
    pub(crate) language: SeedLanguage,
    pub(crate) pairs: Option<&'static PairTable>,
}

impl SuggestionEngine {
    /// Says what the shipped Vietnamese data may say from now on.
    ///
    /// Cheap enough to call on every switch between Vietnamese and English. The
    /// first switch to Vietnamese in the process builds the tables, here rather
    /// than on the first keystroke. `reset` leaves this alone: it is shipped data,
    /// not the user's.
    pub fn set_seed_language(&mut self, language: SeedLanguage) {
        if matches!(language, SeedLanguage::Vietnamese(_)) && self.seed.pairs.is_none() {
            self.seed.pairs = Some(pairs::shipped());
        }
        self.seed.language = language;
    }
}
