//! Onset agreement: the onset–vowel spelling rules of the `c`/`k`/`g`/`gh`/`ngh`
//! families, and the few rhymes that only some onsets ever take.

use crate::unicode::marks::vowel_stem;
use crate::validation::parse::SyllableParts;

/// True when the onset cannot precede the first nucleus vowel under Vietnamese
/// spelling rules (`c` before back vowels only, `gh`/`ngh` before front vowels).
pub(super) fn violates_ckg_spelling(onset: &str, parts: &SyllableParts) -> bool {
    let Some(first) = parts.nucleus_chars().next().and_then(vowel_stem) else {
        return false;
    };
    let stem = first.to_lowercase().next().unwrap_or(first);

    if onset.eq_ignore_ascii_case("c") {
        !matches!(stem, 'a' | 'ă' | 'â' | 'o' | 'ô' | 'ơ' | 'u' | 'ư')
    } else if onset.eq_ignore_ascii_case("g") {
        // `g` + `i` is the valid `gi` digraph (gì, gìn); `g` + e/ê uses `gh`.
        !matches!(stem, 'a' | 'ă' | 'â' | 'o' | 'ô' | 'ơ' | 'u' | 'ư' | 'i')
    } else if onset.eq_ignore_ascii_case("gh") || onset.eq_ignore_ascii_case("ngh") {
        !matches!(stem, 'e' | 'ê' | 'i')
    } else {
        // Native `k` precedes only front vowels (kẻ, kê, kim, kỳ), but loanwords
        // and toponyms break this freely — `Kông` (Hồng Kông), `Kenya` — and a
        // back-vowel `k` is distinct enough from English that allowing it costs
        // little. So `k` is exempt (the rhyme check still applies).
        false
    }
}

/// Rhymes that only some onsets take, each listed with every onset an attested
/// word gives it. Lowercase, toneless; `""` is the empty onset.
///
/// The open `uơ` is attested only in `thuở`, `huơ`, `khuơ`/`khuờ` and `uở`
/// (Viet74K). It is also what Telex's `ưo` + tone resolves to on the way to
/// `ường` (`trưo` + `f` → `truờ` → `trường`), so after any other onset a word
/// that *ends* on it is a typing transient or English, never Vietnamese:
/// `twos` → `tuớ`. (`quơ` is the `qu` onset + `ơ`, not this rhyme.)
const RESTRICTED_RHYMES: &[(&str, &[&str])] = &[("uơ", &["", "h", "kh", "th"])];

/// Whether `rhyme` (toneless, lowercase) may follow `onset`. A rhyme with no row
/// in [`RESTRICTED_RHYMES`] follows any onset.
pub(super) fn onset_admits_rhyme(onset: &str, rhyme: &str) -> bool {
    RESTRICTED_RHYMES
        .iter()
        .find(|(restricted, _)| *restricted == rhyme)
        .is_none_or(|(_, onsets)| onsets.iter().any(|o| o.eq_ignore_ascii_case(onset)))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn open_uo_follows_only_its_attested_onsets() {
        for onset in ["", "h", "kh", "th", "TH", "Kh"] {
            assert!(onset_admits_rhyme(onset, "uơ"), "{onset:?}");
        }
        for onset in ["t", "n", "tr", "gi", "l", "ng"] {
            assert!(!onset_admits_rhyme(onset, "uơ"), "{onset:?}");
        }
    }

    #[test]
    fn unrestricted_rhymes_follow_any_onset() {
        for rhyme in ["ơ", "ươ", "ương", "uôi", "a"] {
            assert!(onset_admits_rhyme("t", rhyme), "{rhyme}");
        }
    }
}
