/// Input-method selector passed to [`crate::apply`] for every keystroke.
///
/// This enum is non-exhaustive so adding another method does not break external
/// consumers. Wire protocols must define their own stable identifiers instead of
/// casting this enum to an integer.
#[non_exhaustive]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum InputMethod {
    /// VNI digit modifiers (`1`–`9`).
    Vni,
    /// Telex letter modifiers without Full Telex shortcuts.
    Telex,
    /// Telex plus leading `w` and the `[` / `]` vowel shortcuts.
    TelexAdvanced,
}

impl InputMethod {
    /// Whether this method shares the Telex modifier grammar.
    #[inline]
    pub const fn is_telex_family(self) -> bool {
        matches!(self, Self::Telex | Self::TelexAdvanced)
    }

    /// Whether Full Telex shortcuts are enabled.
    #[inline]
    pub const fn is_advanced_telex(self) -> bool {
        matches!(self, Self::TelexAdvanced)
    }
}

/// Tone-mark placement style for open glide-initial diphthongs (`oa`, `oe`, `uy`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ToneStyle {
    /// "Kiểu cũ": tone on the first vowel — `hòa`, `khỏe`, `thúy`.
    Traditional,
    /// "Kiểu mới": tone on the main (second) vowel — `hoà`, `khoẻ`, `thuý`.
    #[default]
    Modern,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn telex_family_is_explicit_and_compact() {
        assert!(InputMethod::Telex.is_telex_family());
        assert!(InputMethod::TelexAdvanced.is_telex_family());
        assert!(!InputMethod::Vni.is_telex_family());
        assert!(InputMethod::TelexAdvanced.is_advanced_telex());
        assert_eq!(std::mem::size_of::<InputMethod>(), 1);
    }
}
