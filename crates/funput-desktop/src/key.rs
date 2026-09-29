//! Key model + classification: what a keystroke means for composition.

use funput_engine::KeySource;

use crate::Caret;

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
    /// Enter. Flagged on its own because it is the one key after which the shell
    /// knows where the caret sits — the start of a line — and says so to the engine.
    pub is_enter: bool,
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
    /// navigation, function keys, or a system shortcut. Carries where the key
    /// leaves the caret, for [`crate::ShellState::caret_moved`].
    Flush(Caret),
    /// Irrelevant key (no character, not navigation) — pass through, leave the
    /// composition as-is.
    PassThrough,
}

/// Decide what a key means for composition. Toggle (VI/EN) is handled by the shell
/// *before* this, since the toggle combo is configurable and host-specific.
pub fn classify(ev: &KeyEvent) -> KeyKind {
    if ev.mods.is_shortcut() {
        return KeyKind::Flush(landing(ev));
    }
    if ev.is_backspace {
        return KeyKind::Backspace;
    }
    if ev.is_navigation {
        return KeyKind::Flush(landing(ev));
    }
    match ev.ch {
        Some(c) => KeyKind::Compose(c, ev.source),
        None => KeyKind::PassThrough,
    }
}

/// Where a flushing key leaves the caret. Enter with a modifier still counts — it
/// is how Alt+Enter and Shift+Enter break a line in apps where plain Enter submits.
/// Anything else is a guess, and [`Caret::Unknown`] is the cheap one to get wrong.
fn landing(ev: &KeyEvent) -> Caret {
    if ev.is_enter {
        Caret::LineStart
    } else {
        Caret::Unknown
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
            is_enter: false,
            source: KeySource::Standard,
        }
    }

    fn enter() -> KeyEvent {
        let mut ev = key(Some('\r'));
        ev.is_navigation = true;
        ev.is_enter = true;
        ev
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
        assert_eq!(classify(&ev), KeyKind::Flush(Caret::Unknown)); // Ctrl+A must not compose
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
        assert_eq!(classify(&nav), KeyKind::Flush(Caret::Unknown));
    }

    #[test]
    fn enter_leaves_the_caret_at_a_line_start() {
        assert_eq!(classify(&enter()), KeyKind::Flush(Caret::LineStart));
    }

    /// Alt+Enter and Shift+Enter are how some apps break a line where plain Enter
    /// submits, so the modifier does not change where the caret lands.
    #[test]
    fn enter_with_a_modifier_still_starts_a_line() {
        let mut alt = enter();
        alt.mods.alt = true;
        assert_eq!(classify(&alt), KeyKind::Flush(Caret::LineStart));

        let mut shift = enter();
        shift.mods.shift = true;
        assert_eq!(classify(&shift), KeyKind::Flush(Caret::LineStart));
    }

    #[test]
    fn classify_no_char_passes_through() {
        assert_eq!(classify(&key(None)), KeyKind::PassThrough);
    }
}
