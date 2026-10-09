//! Onset clusters of Tây Nguyên place and ethnic names. Every entry cites a real
//! name that needs it; a cluster is added only with one.

use crate::unicode::marks::is_vowel;

/// How far a cluster onset vouches for the rest of the word.
///
/// Judged on the keys that spell the cluster, not the letters: Telex strokes a
/// `d` from a later one (`droid` → `đroi`), so `đr` shares English's `dr`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum ClusterKind {
    /// English begins words with it too (`draw`, `plow`), so the rhyme must still
    /// be Vietnamese — otherwise auto-restore would leave `draw` as `dră`.
    Shared,
    /// No English word begins with it, so the cluster alone proves the word is a
    /// name and the rhyme is taken as written (`Kpă`, `Dliê`).
    Distinct,
}

const SHARED: &[&str] = &[
    "bh",  // Ea Bhốk
    "bl",  // Blơr
    "br",  // Brâu
    "dr",  // Ea Drăng, Chư Drăng
    "đr",  // M'Đrắk
    "gl",  // Đắk Glong
    "gr",  // Ia Grai
    "kl",  // Ea Kly
    "kr",  // Krông Pắc, Krông Búk
    "phl", // Phlắc Khlá
    "pl",  // Kon Plông, Pleiku
    "pr",  // Chư Prông
];

const DISTINCT: &[&str] = &[
    "dl",  // Cư Dliê M'nông
    "hđr", // Hđrung
    "hm",  // Chư Hmu
    "hn",  // Hning
    "hr",  // Hrê, Chư Hreng
    "kb",  // Kbang
    "kd",  // Ia Kdăm
    "kđr", // Kđrao
    "khl", // Khlá Phlạo
    "km",  // Kmun
    "kp",  // Kpă, Kpăng
    "kt",  // Cư KTy, Ktul
    "ktl", // Ktlê
    "mđh", // Mđhur
    "mr",  // Ia Mrơn
    "rc",  // Chư Rcăm
    "rl",  // Rlâm
    "rs",  // Ia Rsươm
    "tb",  // Tbuăn
    "xr",  // Xrê
    "xt",  // Xtiêng
];

/// The kind of cluster `onset` is, if it is one. Case-blind, `Đ` included.
///
/// Hot path: onset matching asks this of every prefix it tries (`trư`, `tr`), so
/// anything but two or more consonants is turned away before the lists.
pub(crate) fn cluster_kind(onset: &str) -> Option<ClusterKind> {
    if onset.chars().nth(1).is_none() || onset.chars().any(is_vowel) {
        return None;
    }
    let is = |list: &[&str]| list.iter().any(|entry| same_letters(onset, entry));
    if is(DISTINCT) {
        Some(ClusterKind::Distinct)
    } else if is(SHARED) {
        Some(ClusterKind::Shared)
    } else {
        None
    }
}

/// `onset` spells `entry` (lowercase) in any case. An onset carries no diacritic,
/// so ASCII folding plus `Đ` covers every letter one can hold. The byte length
/// (the same for `Đ` and `đ`) rejects most entries up front.
fn same_letters(onset: &str, entry: &str) -> bool {
    if onset.len() != entry.len() {
        return false;
    }
    if onset.is_ascii() {
        return onset.eq_ignore_ascii_case(entry);
    }
    let fold = |c: char| {
        if c == 'Đ' {
            'đ'
        } else {
            c.to_ascii_lowercase()
        }
    };
    onset.chars().map(fold).eq(entry.chars())
}
