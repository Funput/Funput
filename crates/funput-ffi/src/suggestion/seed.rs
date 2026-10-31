//! The shipped Vietnamese data behind a suggestion handle: whether it may speak,
//! and in which tone style it writes.
//!
//! The codes are `SeedLanguage`'s own; this file only names them for C and checks
//! at compile time that the two spellings agree.

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

// Literals above so cbindgen can write them into the header; held to the library's
// codes here, so the C names cannot drift from what `from_wire` decodes.
const _: () = assert!(FUNPUT_SEED_LANGUAGE_OFF == SeedLanguage::WIRE_OFF);
const _: () = assert!(FUNPUT_SEED_LANGUAGE_VIETNAMESE == SeedLanguage::WIRE_VIETNAMESE);

/// Say what the shipped Vietnamese data may say from now on.
///
/// `language` is a `FUNPUT_SEED_LANGUAGE_*` value; any other value means off, since
/// staying silent is never wrong. `tone_style` is the composer's wire byte
/// (`0 = Traditional`, `1 = Modern`, as in `FunputConfig`), and decides how the
/// bar spells `hòa` / `hoà`.
///
/// The handle keeps its own copy of both, apart from the composition engine's
/// config, so the host must call this whenever the active input language or the
/// tone style changes, whatever changed it: the user's toggle, a settings change,
/// or per-app VI/EN memory switching on focus. Route those through the same place
/// that calls `funput_configure`, so the two handles cannot disagree. It is cheap;
/// the first switch to Vietnamese builds the tables here rather than on a
/// keystroke. `funput_suggestion_reset` leaves it alone.
///
/// Returns false for a null handle, or when the call panicked and the boundary
/// abandoned it; the engine then keeps the setting it had.
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
    let language = SeedLanguage::from_wire(language, decode_tone_style(tone_style));
    with_mut(engine, |engine| {
        engine.set_seed_language(language);
        true
    })
}
