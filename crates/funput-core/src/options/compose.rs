//! Everything one keystroke is composed under, bundled into a single value so a
//! new option is one more field here rather than one more parameter on every
//! function of the per-key pipeline.

use super::{InputMethod, ToneStyle};

/// The settings [`crate::apply_with`] composes a keystroke under.
///
/// `Copy` and a few bytes wide, so it travels by value down the per-key hot path.
/// It is non-exhaustive: build it with [`ComposeOptions::new`] and the `with_*`
/// methods, so a future option never breaks a caller.
///
/// ```
/// use funput_core::{ComposeOptions, InputMethod, ToneStyle};
///
/// let options = ComposeOptions::new(InputMethod::Telex)
///     .with_tone_style(ToneStyle::Traditional)
///     .with_spell_check(true);
/// assert!(options.spell_check);
/// ```
#[non_exhaustive]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct ComposeOptions {
    /// The key grammar that classifies each keystroke.
    pub method: InputMethod,
    /// Where a tone lands on the open glide-initial diphthongs (`hòa` / `hoà`).
    pub tone_style: ToneStyle,
    /// Spell-check ("Kiểm tra chính tả"): place a tone, shape or stroke only when the
    /// result can still become a real Vietnamese syllable; otherwise the key passes
    /// through as a literal character (UniKey-style strict diacritics).
    pub spell_check: bool,
}

impl ComposeOptions {
    /// `method` with the default [`ToneStyle`] and spell-check off.
    #[inline]
    pub const fn new(method: InputMethod) -> Self {
        Self {
            method,
            tone_style: ToneStyle::Modern,
            spell_check: false,
        }
    }

    /// These options with `tone_style`.
    #[inline]
    #[must_use]
    pub const fn with_tone_style(mut self, tone_style: ToneStyle) -> Self {
        self.tone_style = tone_style;
        self
    }

    /// These options with spell-check turned on or off.
    #[inline]
    #[must_use]
    pub const fn with_spell_check(mut self, spell_check: bool) -> Self {
        self.spell_check = spell_check;
        self
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn new_matches_the_documented_defaults() {
        let options = ComposeOptions::new(InputMethod::Vni);
        assert_eq!(options.method, InputMethod::Vni);
        assert_eq!(options.tone_style, ToneStyle::default());
        assert!(!options.spell_check);
    }

    #[test]
    fn builders_set_one_field_each() {
        let base = ComposeOptions::new(InputMethod::Telex);
        let styled = base.with_tone_style(ToneStyle::Traditional);
        assert_eq!(styled.tone_style, ToneStyle::Traditional);
        assert_eq!(styled.with_tone_style(base.tone_style), base);
        assert!(base.with_spell_check(true).spell_check);
    }

    #[test]
    fn stays_small_enough_to_pass_by_value() {
        assert!(std::mem::size_of::<ComposeOptions>() <= 8);
    }
}
