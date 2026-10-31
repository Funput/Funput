//! Shipped data for a bar the user has not taught anything yet: Vietnamese first,
//! with a short English supplement for English typed in Vietnamese mode.
//!
//! - `pairs` — the hand-written pair lists Predict falls back on (`vi.txt`, and the
//!   English supplement `en.txt`), one table per language.
//! - `query` — what those lists offer once the user's pairs have no evidence.
//! - `syllables` — the shipped syllable list, indexed for completing a prefix.
//! - `complete` — where pairs and syllables fill the slots a prefix left empty.
//!
//! Off until a shell says which language the user is typing and how they place
//! tones: Vietnamese pairs after an English word would be noise, and `hoà` is a
//! misspelling to someone who chose `hòa`. A shell that never calls
//! [`SuggestionEngine::set_seed_language`] gets exactly the bar it had before.
//!
//! See `docs/features/vietnamese-seed-suggestion.md`.

mod complete;
mod pairs;
mod query;
mod syllables;

use funput_core::ToneStyle;

use crate::engine::SuggestionEngine;
use pairs::ShippedPairs;
use syllables::SyllableIndex;

/// What the shipped data may say, decided by the language the user is typing in.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub enum SeedLanguage {
    /// Nothing: English mode, a field that does not compose, or the shell has not
    /// said. English mode getting the English supplement is a listed extension in
    /// `docs/features/vietnamese-seed-suggestion.md`, not done.
    #[default]
    Off,
    /// Vietnamese mode, tones placed in this style: the Vietnamese pairs and
    /// syllables, and the English pairs for English typed in this mode.
    Vietnamese(ToneStyle),
}

impl SeedLanguage {
    /// The wire code for [`SeedLanguage::Off`], the one every binding sends.
    pub const WIRE_OFF: u8 = 0;
    /// The wire code for [`SeedLanguage::Vietnamese`].
    pub const WIRE_VIETNAMESE: u8 = 1;

    /// Decodes a binding's language code, with the tone style the binding has
    /// already decoded its own way. Any code not named above is off: staying
    /// silent is never wrong, and an older library meeting a newer host's code
    /// must not guess.
    pub fn from_wire(code: u8, style: ToneStyle) -> Self {
        match code {
            Self::WIRE_VIETNAMESE => Self::Vietnamese(style),
            _ => Self::Off,
        }
    }
}

/// What an engine holds of the shipped data: nothing, or the style to write in
/// together with every table Vietnamese mode reads. One value, so there is
/// no state in which the data is on but its tables were never built.
#[derive(Clone, Copy, Default)]
pub(crate) enum SeedSlot {
    #[default]
    Off,
    Vietnamese {
        style: ToneStyle,
        pairs: &'static ShippedPairs,
        syllables: &'static SyllableIndex,
    },
}

impl SuggestionEngine {
    /// Says what the shipped Vietnamese data may say from now on.
    ///
    /// Cheap enough to call on every switch between Vietnamese and English. The
    /// first switch to Vietnamese in the process builds the tables, here rather
    /// than on the first keystroke. `reset` leaves this alone: it is shipped data,
    /// not the user's.
    pub fn set_seed_language(&mut self, language: SeedLanguage) {
        self.seed = match language {
            SeedLanguage::Off => SeedSlot::Off,
            SeedLanguage::Vietnamese(style) => SeedSlot::Vietnamese {
                style,
                pairs: pairs::shipped(),
                syllables: syllables::shipped(),
            },
        };
    }
}

#[cfg(test)]
mod tests {
    use funput_core::ToneStyle::{Modern, Traditional};

    use super::SeedLanguage;

    #[test]
    fn the_wire_codes_decode_and_anything_unknown_is_off() {
        assert_eq!(
            SeedLanguage::from_wire(SeedLanguage::WIRE_VIETNAMESE, Modern),
            SeedLanguage::Vietnamese(Modern)
        );
        assert_eq!(
            SeedLanguage::from_wire(SeedLanguage::WIRE_VIETNAMESE, Traditional),
            SeedLanguage::Vietnamese(Traditional)
        );
        for code in [SeedLanguage::WIRE_OFF, 2, 7, u8::MAX] {
            assert_eq!(
                SeedLanguage::from_wire(code, Modern),
                SeedLanguage::Off,
                "{code}"
            );
        }
    }
}
