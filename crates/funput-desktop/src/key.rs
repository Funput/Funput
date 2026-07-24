//! Key model + classification: what a keystroke means for composition.

use funput_engine::KeySource;

/// Modifier keys held when a key is pressed. `shift` is tracked but does **not**
/// by itself mark a system shortcut (Shift is part of normal typing).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Mods {
    pub ctrl: bool,
    pub alt: bool,
    pub win: bool,
    pub shift: bool,
}

impl Mods {
    /// A non-Shift modifier is held, i.e. the key is part of a system shortcut
    /// (Ctrl+A, Alt+Tab, Win+…) and must not be composed.
    pub fn is_shortcut(&self) -> bool {
        self.ctrl || self.alt || self.win
    }
}

/// A normalized key event the shell feeds the classifier. `ch` is the character
/// the key would produce (from `ToUnicodeEx` on Windows), if any.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct KeyEvent {
    pub mods: Mods,
    pub ch: Option<char>,
    /// Backspace / Delete-back.
    pub is_backspace: bool,
    /// Caret-moving or non-text key: arrows, Home/End, PageUp/Down, Esc, Delete,
    /// Insert, F-keys, Enter, Tab.
    pub is_navigation: bool,
    /// Where the key physically came from. A numpad digit carries
    /// [`KeySource::Numpad`] so the engine keeps it a literal number instead of a
    /// VNI tone/shape modifier; ordinary keys are [`KeySource::Standard`].
    pub source: KeySource,
}

/// What the shell should do with a key while Vietnamese mode is on.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum KeyKind {
    /// Feed this character to the engine (printable text key, incl. space/punct —
    /// the engine itself decides word boundaries). Carries the key's [`KeySource`]
    /// so a numpad digit is composed as a literal number.
    Compose(char, KeySource),
    /// Backspace pressed — call `engine.backspace()` and apply its result.
    Backspace,
    /// Flush the composition (commit/clear) and let the key pass through —
    /// navigation, function keys, or a system shortcut.
    Flush,
    /// Irrelevant key (no character, not navigation) — pass through, leave the
    /// composition as-is.
    PassThrough,
}

/// Decide what a key means for composition. Toggle (VI/EN) is handled by the shell
/// *before* this, since the toggle combo is configurable and host-specific.
pub fn classify(ev: &KeyEvent) -> KeyKind {
    if ev.mods.is_shortcut() {
        return KeyKind::Flush;
    }
    if ev.is_backspace {
        return KeyKind::Backspace;
    }
    if ev.is_navigation {
        return KeyKind::Flush;
    }
    match ev.ch {
        Some(c) => KeyKind::Compose(c, ev.source),
        None => KeyKind::PassThrough,
    }
}

#[cfg(test)]
mod tests {
    use funput_engine::KeySource;

    use super::*;

    fn key(ch: Option<char>) -> KeyEvent {
        KeyEvent {
            mods: Mods::default(),
            ch,
            is_backspace: false,
            is_navigation: false,
            source: KeySource::Standard,
        }
    }

    #[test]
    fn classify_printable_composes() {
        let std = KeySource::Standard;
        assert_eq!(classify(&key(Some('a'))), KeyKind::Compose('a', std));
        assert_eq!(classify(&key(Some(' '))), KeyKind::Compose(' ', std)); // boundary → engine decides
        assert_eq!(classify(&key(Some('1'))), KeyKind::Compose('1', std));
    }

    #[test]
    fn classify_preserves_numpad_source() {
        // A numpad digit reaches the engine tagged as `Numpad` so it stays a literal
        // number; the top-row digit stays `Standard` (a VNI modifier).
        let mut ev = key(Some('1'));
        ev.source = KeySource::Numpad;
        assert_eq!(classify(&ev), KeyKind::Compose('1', KeySource::Numpad));
    }

    #[test]
    fn classify_shortcut_flushes() {
        let mut ev = key(Some('a'));
        ev.mods.ctrl = true;
        assert_eq!(classify(&ev), KeyKind::Flush); // Ctrl+A must not compose
    }

    #[test]
    fn classify_shift_still_composes() {
        let mut ev = key(Some('A'));
        ev.mods.shift = true;
        assert_eq!(classify(&ev), KeyKind::Compose('A', KeySource::Standard));
    }

    #[test]
    fn classify_backspace_and_navigation() {
        let mut bs = key(None);
        bs.is_backspace = true;
        assert_eq!(classify(&bs), KeyKind::Backspace);

        let mut nav = key(None);
        nav.is_navigation = true;
        assert_eq!(classify(&nav), KeyKind::Flush);
    }

    #[test]
    fn classify_no_char_passes_through() {
        assert_eq!(classify(&key(None)), KeyKind::PassThrough);
    }
}
