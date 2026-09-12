//! Changing the case of text that already exists.
//!
//! Five transforms a person runs over a paragraph they have already typed:
//! UPPERCASE, lowercase, bỏ dấu, sentence case and title case. None of them belongs
//! to the typing engine — no keystroke reaches this module, nothing here keeps
//! state, and the whole surface is [`apply`].
//!
//! **Why core rather than a shell.** The Chuyển mã window will offer these on three
//! platforms and `funput case` offers them everywhere else. What counts as the end
//! of a sentence, and which letters lose their diacritics, then has to be one copy
//! or it drifts — the same argument that put [`crate::charset`] here rather than in
//! the windows that draw it. The decisions themselves live in
//! `docs/features/text-case.md`.
//!
//! **Why its own feature and not `charset`'s.** A keyboard has no use for the
//! legacy charset tables, but it may well want bỏ dấu one day: iOS and Android can
//! both read the selected text, which is the one place a case transform needs no
//! permission at all. Folding the two features together would make that choice cost
//! four codecs. Going the other way, `funput-config` enables `charset` only to read
//! a UniKey macro file and never changes anyone's case.
//!
//! [`Transform`] grows one variant per transform as each is implemented, so that a
//! shell matching on it cannot compile against a variant that does nothing yet.

mod case;
mod diacritics;

/// Which transform to run.
///
/// Not `#[non_exhaustive]` on purpose: a shell that matches every variant should
/// stop compiling when a new one arrives, because a new transform means a new menu
/// entry for it to draw.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Transform {
    /// `Xin chào Việt Nam` → `XIN CHÀO VIỆT NAM`.
    Upper,
    /// `Xin Chào Việt Nam` → `xin chào việt nam`.
    Lower,
    /// `Tiếng Việt rất đẹp` → `Tieng Viet rat dep`.
    NoDiacritics,
}

/// The switches a transform reads.
///
/// One struct for all five rather than an argument list per transform: a shell
/// stores these next to the window and hands the same value over whichever button
/// the user pressed, and a new switch then costs no signature change.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Options {
    /// Whether bỏ dấu also unstrokes `đ`/`Đ` into `d`/`D`.
    ///
    /// On by default: `dep` is what nearly everyone means by bỏ dấu. Off keeps the
    /// stroke, for a filename on a system that accepts it.
    pub d_to_ascii: bool,
    /// Whether title case leaves a word that is already all-caps alone.
    ///
    /// On by default, so `TP. HCM` does not become `Tp. Hcm`.
    pub keep_all_caps: bool,
}

impl Default for Options {
    fn default() -> Self {
        Self {
            d_to_ascii: true,
            keep_all_caps: true,
        }
    }
}

/// Run `transform` over `text`.
///
/// Returns a fresh `String` rather than editing in place: a transform can change
/// how many bytes a character takes, and the caller is a window showing the before
/// and the after side by side.
pub fn apply(text: &str, transform: Transform, options: Options) -> String {
    match transform {
        Transform::Upper => case::upper(text, options),
        Transform::Lower => case::lower(text, options),
        Transform::NoDiacritics => diacritics::strip(text, options),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn options_default_to_the_common_answer() {
        let options = Options::default();
        assert!(options.d_to_ascii);
        assert!(options.keep_all_caps);
    }

    #[test]
    fn apply_dispatches() {
        let options = Options::default();
        assert_eq!(apply("Việt", Transform::Upper, options), "VIỆT");
        assert_eq!(apply("Việt", Transform::Lower, options), "việt");
    }
}
