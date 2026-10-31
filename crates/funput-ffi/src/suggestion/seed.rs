//! The shipped Vietnamese data behind a suggestion handle: whether it may speak,
//! and in which tone style it writes.

use funput_suggestions::SeedLanguage;

use super::FunputSuggestionEngine;
use super::engine::with_mut;
use crate::engine::decode_tone_style;

/// [`funput_suggestion_set_seed_language`]: the user is typing English, or the
/// host has not said. Nothing shipped is offered.
pub const FUNPUT_SEED_LANGUAGE_OFF: u8 = 0;
/// [`funput_suggestion_set_seed_language`]: the user is typing Vietnamese, so the
/// shipped pairs may predict the word after a space.
pub const FUNPUT_SEED_LANGUAGE_VIETNAMESE: u8 = 1;

/// Say what the shipped Vietnamese data may say from now on.
///
/// `language` is a `FUNPUT_SEED_LANGUAGE_*` value; any other value means off, since
/// staying silent is never wrong. `tone_style` is the composer's wire byte
/// (`0 = Traditional`, `1 = Modern`, as in `FunputConfig`), and decides how the
/// bar spells `hòa` / `hoà`. Call it when the handle is created and whenever the
/// user switches between Vietnamese and English or changes the tone style: it is
/// cheap, and the first switch to Vietnamese builds the tables here rather than on
/// a keystroke. `funput_suggestion_reset` leaves it alone.
///
/// Returns false only for a null handle.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used
/// concurrently.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_set_seed_language(
    engine: *mut FunputSuggestionEngine,
    language: u8,
    tone_style: u8,
) -> bool {
    let language = decode(language, tone_style);
    with_mut(engine, |engine| {
        engine.set_seed_language(language);
        true
    })
}

fn decode(language: u8, tone_style: u8) -> SeedLanguage {
    match language {
        FUNPUT_SEED_LANGUAGE_VIETNAMESE => SeedLanguage::Vietnamese(decode_tone_style(tone_style)),
        _ => SeedLanguage::Off,
    }
}

#[cfg(test)]
mod tests {
    use funput_core::ToneStyle;
    use funput_suggestions::SeedLanguage;

    use super::{FUNPUT_SEED_LANGUAGE_OFF, FUNPUT_SEED_LANGUAGE_VIETNAMESE, decode};

    #[test]
    fn the_wire_bytes_decode_and_anything_unknown_is_off() {
        assert_eq!(
            decode(FUNPUT_SEED_LANGUAGE_VIETNAMESE, 1),
            SeedLanguage::Vietnamese(ToneStyle::Modern)
        );
        assert_eq!(
            decode(FUNPUT_SEED_LANGUAGE_VIETNAMESE, 0),
            SeedLanguage::Vietnamese(ToneStyle::Traditional)
        );
        for language in [FUNPUT_SEED_LANGUAGE_OFF, 2, 7, u8::MAX] {
            assert_eq!(decode(language, 1), SeedLanguage::Off, "{language}");
        }
    }
}
