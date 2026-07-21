//! Onset–vowel spelling agreement for the `c`/`k`/`g`/`gh`/`ngh` families.

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
