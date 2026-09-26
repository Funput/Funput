//! Tây Nguyên place and ethnic names: the spellings they use that native
//! Vietnamese does not — `Krông Pắc`, `Kpă`, `Xtiêng`, `Đắk Lắk`.
//!
//! Every exception lives here, next to the names that justify it, and each is
//! only as wide as English allows: auto-restore must still turn `draw`, `cool`
//! and `know` back into English. What is accepted:
//!
//! - **Cluster onsets** ([`cluster_kind`]). Those English also spells (`kr`,
//!   `pl`) keep the Vietnamese rhyme check; those it never does (`kp`, `hr`,
//!   `xt`) prove the word is a name, so any rhyme passes (`Kpă`, `Dliê`).
//! - **Final `k`** read as `c` (`Đắk`, `Lắk`), in [`super::coda::normalized_coda`].
//!
//! - **Finals `h`, `l`, `r`** ([`closes_with_name_final`]) — `Chư Păh`, `Ea Nuôl`,
//!   `Blơr` — in VNI only. There only a digit composes, so English never reaches
//!   them; in Telex they are English `cash` (`s` + `h` → `cáh`, `aha` → `âh`),
//!   `cool` (`côl`) and the hỏi key.
//!
//! Left out on purpose, because Telex could no longer restore English: `kn` and
//! `sl` onsets (`know` → `knơ`, `slow` → `slơ`), and a stop coda without a tone
//! (`moot` → `môt`).

mod clusters;

pub(crate) use clusters::{ClusterKind, cluster_kind};

/// Finals only place names use: `h` (`Chư Păh`), `l` (`Ea Nuôl`), `r` (`Blơr`).
const NAME_FINALS: [char; 3] = ['h', 'l', 'r'];

/// First letters of the native codas: `ng`/`nh` begin with `n`, `ch` with `c`.
const CODA_INITIALS: [char; 5] = ['c', 'm', 'n', 'p', 't'];

/// Longer than any nucleus (`uyê`, `ươi`), so a longer one closes nothing.
const MAX_NUCLEUS: usize = 4;

/// Whether a rhyme the Vietnamese inventory rejects still stands in a name:
/// after a distinct cluster any rhyme does (`Kpă`, `Dliê`). The caller has
/// already checked the order and the coda.
pub(crate) fn admits(onset: &str) -> bool {
    cluster_kind(onset) == Some(ClusterKind::Distinct)
}

/// True when `coda` is a name final and `nucleus` is a vowel Vietnamese closes
/// with a consonant (`ăh` like `ăn`, `uôl` like `uôn`). These finals are
/// fricatives and sonorants, not stops, so any tone goes. `nucleus` is spelled
/// the way `known` reads it, and `known` answers whether an inventory rhyme
/// starts with its argument.
pub(crate) fn closes_with_name_final(
    nucleus: impl Iterator<Item = char>,
    coda: &[char],
    known: impl Fn(&[char]) -> bool,
) -> bool {
    let [last] = coda else {
        return false;
    };
    if !NAME_FINALS.contains(last) {
        return false;
    }
    let mut query = ['\0'; MAX_NUCLEUS + 1];
    let mut len = 0;
    for ch in nucleus {
        if len == MAX_NUCLEUS {
            return false;
        }
        query[len] = ch;
        len += 1;
    }
    len > 0
        && CODA_INITIALS.iter().any(|&initial| {
            query[len] = initial;
            known(&query[..=len])
        })
}

#[cfg(test)]
mod tests;
