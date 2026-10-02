//! Onsets Vietnamese spelling does not have, admitted on request: the UniKey /
//! OpenKey switch "Cho phép phụ âm đầu Z, F, W, J".
//!
//! Each is a consonant that teencode, loanwords and some ethnic names open a
//! syllable with. Admitting one only widens the **onset** — the rhyme must still
//! be Vietnamese, so `fôd` (food) and `jump` stay English.

/// A set of extra onsets, admitted on top of the native inventory.
///
/// A plain bitset: combine with [`ExtraOnsets::union`], test with
/// [`ExtraOnsets::contains`]. The default is [`ExtraOnsets::NONE`].
///
/// ```
/// use funput_core::ExtraOnsets;
///
/// assert!(ExtraOnsets::ZFWJ.contains(ExtraOnsets::J));
/// assert!(!ExtraOnsets::F.union(ExtraOnsets::Z).contains(ExtraOnsets::W));
/// ```
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Default)]
pub struct ExtraOnsets(u8);

impl ExtraOnsets {
    /// No extra onset: native spelling only.
    pub const NONE: Self = Self(0);
    /// `f`, as in `fải` (teencode `phải`).
    pub const F: Self = Self(1);
    /// `j`, as in `jờ` (teencode `giờ`) and the district name Cư Jút.
    pub const J: Self = Self(1 << 1);
    /// `w`, as in `wá` (teencode `quá`).
    pub const W: Self = Self(1 << 2);
    /// `z`, as in `zô` (teencode `vô`).
    pub const Z: Self = Self(1 << 3);
    /// The four letters UniKey and OpenKey admit behind one switch.
    pub const ZFWJ: Self = Self::Z.union(Self::F).union(Self::W).union(Self::J);

    /// Every onset in `self` or in `other`.
    #[inline]
    #[must_use]
    pub const fn union(self, other: Self) -> Self {
        Self(self.0 | other.0)
    }

    /// Whether every onset in `other` is also in `self`.
    #[inline]
    pub const fn contains(self, other: Self) -> bool {
        self.0 & other.0 == other.0
    }

    /// Whether no extra onset is admitted.
    #[inline]
    pub const fn is_empty(self) -> bool {
        self.0 == 0
    }

    /// Whether `prefix` spells one of these onsets, in any case.
    ///
    /// Hot path: onset matching asks this of each prefix the native inventory
    /// turned away, so an empty set answers before the table is read.
    #[inline]
    pub(crate) fn admits(self, prefix: &str) -> bool {
        !self.is_empty()
            && SPELLINGS.iter().any(|&(onset, spelling)| {
                self.contains(onset) && prefix.eq_ignore_ascii_case(spelling)
            })
    }
}

/// How each extra onset is spelled. Adding one is a constant above and a row here;
/// a longer spelling (say `dz`) wins over a native prefix (`d`) because onset
/// matching tries the longest prefix first.
const SPELLINGS: [(ExtraOnsets, &str); 4] = [
    (ExtraOnsets::F, "f"),
    (ExtraOnsets::J, "j"),
    (ExtraOnsets::W, "w"),
    (ExtraOnsets::Z, "z"),
];
