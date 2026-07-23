//! Physical origin of a keystroke.
//!
//! The same `char` means different things depending on where it is typed: on the
//! main keyboard a VNI digit is a tone/shape modifier (`a` + `1` → `á`), but on the
//! numeric keypad the user wants the literal number. The platform shell knows the
//! origin (macOS `NSEvent` `.numericPad`, Windows `VK_NUMPAD0..9`, X11 keypad
//! keysyms) and reports it here, since the raw scalar cannot carry that distinction.
//!
//! Extensible by design: new origins (e.g. an on-screen keypad) add a variant here
//! and a rule below — nothing downstream needs to branch on the platform.

/// Where a keystroke came from, as reported by the platform shell.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum KeySource {
    /// Main keyboard area (the default). Digits may act as VNI modifiers.
    #[default]
    Standard,
    /// Numeric keypad — a digit here is always a literal number, never a modifier.
    Numpad,
}

impl KeySource {
    /// True when this key must be emitted as a literal number rather than
    /// interpreted as a Vietnamese tone/shape modifier (i.e. a numpad digit).
    #[inline]
    pub(crate) fn forces_literal_digit(self, key: char) -> bool {
        matches!(self, Self::Numpad) && key.is_ascii_digit()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn numpad_digit_forces_literal() {
        assert!(KeySource::Numpad.forces_literal_digit('0'));
        assert!(KeySource::Numpad.forces_literal_digit('5'));
        assert!(KeySource::Numpad.forces_literal_digit('9'));
    }

    #[test]
    fn numpad_non_digit_is_not_forced() {
        // Keypad `+ - * / .` and any letter stay on their normal path.
        assert!(!KeySource::Numpad.forces_literal_digit('+'));
        assert!(!KeySource::Numpad.forces_literal_digit('a'));
    }

    #[test]
    fn standard_digit_is_never_forced() {
        // Top-row digits keep their VNI-modifier role.
        assert!(!KeySource::Standard.forces_literal_digit('5'));
    }

    #[test]
    fn default_source_is_standard() {
        assert_eq!(KeySource::default(), KeySource::Standard);
    }
}
