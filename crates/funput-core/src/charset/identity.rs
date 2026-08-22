//! Which charset this is, what to call it, and how to write it down.
//!
//! Three things a consumer cannot work out for itself, because [`Charset`] is
//! `#[non_exhaustive]`: code outside this crate has to match with a wildcard arm, so
//! it would silently miss a variant added later. Everything here is forced by the
//! compiler instead — [`ALL`] by the guard at the bottom, the two names by their
//! exhaustive matches.
//!
//! [`Charset::name`] is for a person and [`Charset::slug`] is for a machine, and the
//! split is the point. A name may be reworded; a slug is a command-line value and a
//! setting written to disk, so it may not be.

/// A Vietnamese character encoding.
///
/// `#[non_exhaustive]`: VIQR, VISCII and the rest are out of scope today but not
/// forever, and adding one should not break callers. Match with a wildcard arm.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum Charset {
    /// Precomposed Unicode (NFC) — the pivot, and what every modern system uses.
    Unicode,
    /// TCVN3, also called ABC: the `.VnTime` encoding of Vietnamese government
    /// documents. One byte per letter, and no code for an uppercase toned vowel.
    Tcvn3,
    /// VNI-Windows, the `VNI-Times` encoding. Spells most letters as a base byte
    /// plus a mark byte, so a letter is not a character and conversion moves
    /// character boundaries. Named for the encoding rather than `Vni`, which would
    /// sit confusingly beside [`crate::InputMethod::Vni`] — a way of *typing*, not
    /// a way of storing.
    VniWindows,
    /// Unicode tổ hợp, UniKey's convention: the shaped vowel stays precomposed and
    /// only the **tone** rides as a combining mark, so `ậ` is `â` plus `U+0323`.
    ///
    /// Deliberately not called `Decomposed`: this is not NFD, which would take the
    /// shape apart too. Reading accepts NFD anyway — see the codec — but writing
    /// only ever produces this form.
    UnicodeCombining,
}

impl Charset {
    /// What to call this charset in front of a user.
    ///
    /// The encoding's own name rather than interface copy — `TCVN3 (ABC)` reads the
    /// same in any language, and these are the names UniKey uses, which is what
    /// Vietnamese users already know them by. Keeping them here is what stops a menu
    /// on Windows and one on Linux from drifting apart.
    pub const fn name(self) -> &'static str {
        match self {
            Self::Unicode => "Unicode dựng sẵn",
            Self::Tcvn3 => "TCVN3 (ABC)",
            Self::VniWindows => "VNI-Windows",
            Self::UnicodeCombining => "Unicode tổ hợp",
        }
    }

    /// How this charset is written down: on a command line, in a config file,
    /// anywhere a machine reads it back.
    ///
    /// ASCII, lowercase, and mirroring the variant name, so it is guessable from the
    /// documentation and typeable without a Vietnamese keyboard. **Unlike
    /// [`name`](Self::name), a slug is a contract.** It ends up in a saved setting and
    /// in a script someone wrote a year ago, so it may not be reworded — which is also
    /// why it is not derived from the name.
    ///
    /// A consumer needs no lookup function for the other direction:
    /// `ALL.iter().find(|c| c.slug() == input)`.
    pub const fn slug(self) -> &'static str {
        match self {
            Self::Unicode => "unicode",
            Self::Tcvn3 => "tcvn3",
            Self::VniWindows => "vni-windows",
            Self::UnicodeCombining => "unicode-combining",
        }
    }
}

/// Every charset, in the order an interface should offer them.
///
/// A caller cannot build this list for itself: `Charset` is `#[non_exhaustive]`, so
/// code outside this crate must write a wildcard arm and would silently miss a
/// variant added later. [`detect`] scores exactly this list, so a charset a user can
/// choose is one the tool can also recognise.
pub const ALL: [Charset; 4] = [
    Charset::Unicode,
    Charset::Tcvn3,
    Charset::VniWindows,
    Charset::UnicodeCombining,
];

/// Adding a variant must break the build here rather than drop it out of every menu.
///
/// [`Charset::name`] already forces a name for each one; this catches the other
/// half — a variant that has a name but never made it into [`ALL`].
const _: () = {
    const fn slot(charset: Charset) -> u32 {
        match charset {
            Charset::Unicode => 0,
            Charset::Tcvn3 => 1,
            Charset::VniWindows => 2,
            Charset::UnicodeCombining => 3,
        }
    }
    let (mut listed, mut i) = (0u32, 0);
    while i < ALL.len() {
        listed |= 1 << slot(ALL[i]);
        i += 1;
    }
    assert!(
        listed == (1 << ALL.len()) - 1,
        "a charset is missing from ALL"
    );
};

#[cfg(test)]
mod tests {
    use super::*;

    /// Two menu entries reading the same, or a blank one, are the failures a shared
    /// list exists to make impossible.
    #[test]
    fn every_charset_has_a_distinct_name() {
        let mut names: Vec<&str> = ALL.iter().map(|c| c.name()).collect();
        assert!(names.iter().all(|n| !n.is_empty()));
        names.sort_unstable();
        names.dedup();
        assert_eq!(names.len(), ALL.len(), "two charsets share a name");
    }

    /// The two Unicode charsets are the pair a user actually has to choose between,
    /// so neither may be called just "Unicode".
    #[test]
    fn the_two_unicode_charsets_are_told_apart_by_name() {
        assert_ne!(Charset::Unicode.name(), Charset::UnicodeCombining.name());
        assert!(Charset::Unicode.name().contains("dựng sẵn"));
        assert!(Charset::UnicodeCombining.name().contains("tổ hợp"));
    }

    /// A slug is typed at a shell prompt and stored in config files, so anything but
    /// lowercase ASCII would be a trap somewhere.
    #[test]
    fn every_slug_is_distinct_and_typeable() {
        let mut slugs: Vec<&str> = ALL.iter().map(|c| c.slug()).collect();
        for slug in &slugs {
            assert!(!slug.is_empty());
            assert!(
                slug.bytes()
                    .all(|b| b.is_ascii_lowercase() || b.is_ascii_digit() || b == b'-'),
                "{slug} is not lowercase ASCII"
            );
        }
        slugs.sort_unstable();
        slugs.dedup();
        assert_eq!(slugs.len(), ALL.len(), "two charsets share a slug");
    }

    /// The lookup this crate deliberately does not provide, shown to work — a
    /// consumer builds it from `ALL` rather than being given a function.
    #[test]
    fn a_slug_finds_its_charset_again() {
        for charset in ALL {
            let found = ALL.iter().find(|c| c.slug() == charset.slug());
            assert_eq!(found, Some(&charset));
        }
        assert!(ALL.iter().all(|c| c.slug() != "viscii"));
    }

    /// Pins the published values. Rewording one breaks a saved setting and a script
    /// someone wrote a year ago, so it has to break this first.
    #[test]
    fn the_published_slugs_do_not_change() {
        assert_eq!(Charset::Unicode.slug(), "unicode");
        assert_eq!(Charset::Tcvn3.slug(), "tcvn3");
        assert_eq!(Charset::VniWindows.slug(), "vni-windows");
        assert_eq!(Charset::UnicodeCombining.slug(), "unicode-combining");
    }
}
