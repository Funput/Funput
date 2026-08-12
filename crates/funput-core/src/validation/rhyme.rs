//! Vietnamese rhyme (vần) inventory — the toneless nucleus+coda part of a
//! syllable. A composed syllable "exists in Vietnamese" only if its rhyme is in
//! this set (plus a valid onset and tone). This is what lets the engine keep
//! real syllables and revert the rest, without a full word corpus.
//!
//! Generous on purpose: when unsure, a rhyme is **included** so real Vietnamese
//! is never wrongly reverted. Report any real word whose rhyme is missing — it is
//! a one-line addition here.
//!
//! Completeness is cross-checked against an attested Vietnamese word corpus by
//! `tests/spellcheck_corpus.rs`; rhymes for established loanwords and onomatopoeia
//! (`buýt`, `xoong`, `soóc`, `tuýp`, `giếng`/`giêng`, `oăm`, `huých`…) are included
//! so spell-check ("Kiểm tra chính tả") never blocks a real word.
//!
//! The inventory is queried three ways, all O(log n) over a sorted view built
//! once: as written ([`is_valid_rhyme`], [`has_prefix`]) and **deshaped**
//! ([`matches_deshaped`], [`has_deshaped_prefix`]) — the latter for the stretch
//! of typing before the shape keys land, where `uoc` still stands for `ươc`.

use std::sync::LazyLock;

use crate::unicode::marks::vowel_stem;

/// Valid toneless rhymes (lowercase, shaped vowels: `ươ`, `iê`, …).
pub(crate) const VALID_RHYMES: &[&str] = &[
    // Open (no coda)
    "a", "e", "ê", "i", "o", "ô", "ơ", "u", "ư", "y", "ia", "ya", "ai", "ao", "au", "ay", "âu",
    "ây", "eo", "êu", "iu", "oa", "oe", "oi", "ôi", "ơi", "ua", "ui", "uê", "uy", "uơ", "ưa", "ưi",
    "ưu", "oai", "oay", "oeo", "uôi", "uây", "uya", "uyu", "ươi", "ươu", "iêu", "yêu", "oao",
    "uêu", // -m
    "am", "ăm", "âm", "em", "êm", "im", "om", "ôm", "ơm", "um", "iêm", "yêm", "uôm", "ươm", "oam",
    "oăm", "oem", // -p
    "ap", "ăp", "âp", "ep", "êp", "ip", "op", "ôp", "ơp", "up", "iêp", "ươp", "oap", "uyp",
    // -n
    "an", "ăn", "ân", "en", "ên", "in", "on", "ôn", "ơn", "un", "ưn", "iên", "yên", "uôn", "ươn",
    "oan", "oăn", "oen", "uân", "uyên", "uyn", // -t
    "at", "ăt", "ât", "et", "êt", "it", "ot", "ôt", "ơt", "ut", "ưt", "iêt", "yêt", "uôt", "ươt",
    "oat", "oăt", "oet", "uât", "uyêt", "yt", "uyt", // -ng
    "ang", "ăng", "âng", "eng", "ong", "ông", "ung", "ưng", "iêng", "uông", "ương", "oang", "oăng",
    "uâng", "oong", "êng", "yêng", "ơng", // -c
    "ac", "ăc", "âc", "oc", "ôc", "uc", "ưc", "iêc", "uôc", "ươc", "oac", "oăc", "ec", "ooc",
    // -nh
    "anh", "ênh", "inh", "ynh", "uynh", "uênh", "oanh", // -ch
    "ach", "êch", "ich", "uêch", "oach", "uych",
];

/// The inventory sorted once for O(log n) membership checks. [`VALID_RHYMES`]
/// itself stays grouped by coda — that grouping is the human-readable
/// documentation of the inventory.
static SORTED_RHYMES: LazyLock<Vec<&'static str>> = LazyLock::new(|| {
    let mut sorted = VALID_RHYMES.to_vec();
    sorted.sort_unstable();
    sorted
});

/// The same inventory with every vowel shape stripped (`ươc` → `uoc`), sorted.
/// This is the inventory as the user's *keystrokes* spell it before the shape
/// keys land, which is what [`has_deshaped_prefix`] and [`matches_deshaped`]
/// answer questions about — computed once so neither re-deshapes ~166 rhymes.
static DESHAPED_RHYMES: LazyLock<Vec<String>> = LazyLock::new(|| {
    let mut deshaped: Vec<String> = VALID_RHYMES
        .iter()
        .map(|r| r.chars().map(plain_base).collect())
        .collect();
    deshaped.sort_unstable();
    deshaped
});

/// Plain base of a vowel — tone **and** shape stripped (`ớ`→`o`, `ẽ`→`e`,
/// `ư`→`u`). Non-vowels pass through lowercased.
pub(crate) fn plain_base(c: char) -> char {
    let stem = vowel_stem(c).unwrap_or(c);
    match char::to_lowercase(stem).next().unwrap_or(stem) {
        'ă' | 'â' => 'a',
        'ê' => 'e',
        'ô' | 'ơ' => 'o',
        'ư' => 'u',
        other => other,
    }
}

/// True if `rhyme` (toneless, lowercase) is a valid Vietnamese rhyme.
pub fn is_valid_rhyme(rhyme: &str) -> bool {
    SORTED_RHYMES.binary_search(&rhyme).is_ok()
}

/// True if `rhyme` (toneless, lowercase) *is* an inventory rhyme once both are
/// deshaped: `ien` matches `iên`, `uoc` matches `uôc`/`ươc`, `uu` matches `ưu`.
///
/// A whole match, not a prefix: the caller is looking at a word the user already
/// finished, so `ie` — which no rhyme spells — is English `die`, not a `diên` the
/// user is halfway through.
pub(crate) fn matches_deshaped(rhyme: &str) -> bool {
    DESHAPED_RHYMES
        .binary_search_by(|entry| entry.chars().cmp(rhyme.chars().map(plain_base)))
        .is_ok()
}

/// Whether some deshaped rhyme starts with `prefix` (already deshaped). The
/// inventory is sorted, so all candidates form a contiguous range beginning at
/// the first entry ≥ `prefix`; checking that single entry suffices.
pub(crate) fn has_deshaped_prefix(prefix: &[char]) -> bool {
    let first = DESHAPED_RHYMES
        .partition_point(|r| r.chars().cmp(prefix.iter().copied()) == std::cmp::Ordering::Less);
    DESHAPED_RHYMES
        .get(first)
        .is_some_and(|r| starts_with(r, prefix))
}

fn starts_with(rhyme: &str, prefix: &[char]) -> bool {
    let mut chars = rhyme.chars();
    prefix.iter().all(|&p| chars.next() == Some(p))
}

/// Whether any valid shaped rhyme starts with `prefix`.
///
/// Used by free-position shape candidates, where deshaping would over-accept an
/// impossible rhyme such as `ôe` merely because plain `oe` exists.
pub(crate) fn has_prefix(prefix: &[char]) -> bool {
    let first = SORTED_RHYMES.partition_point(|rhyme| {
        rhyme.chars().cmp(prefix.iter().copied()) == std::cmp::Ordering::Less
    });
    SORTED_RHYMES
        .get(first)
        .is_some_and(|rhyme| starts_with(rhyme, prefix))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn known_rhymes_present() {
        for ok in [
            "a", "ương", "iêt", "uyên", "uyêt", "anh", "ach", "uêch", "uyu", "uya", "ưng", "oăn",
            "uâng", "ynh",
            // Loanword / onomatopoeia rhymes added for spell-check completeness.
            "uyt", "yt", "oong", "ooc", "ec", "êng", "yêng", "oăm", "uych", "uyp", "uyn", "uêu",
        ] {
            assert!(is_valid_rhyme(ok), "{ok} should be a valid rhyme");
        }
    }

    #[test]
    fn nonexistent_rhymes_absent() {
        for bad in ["eg", "id", "ub", "az", "onk", "erf"] {
            assert!(!is_valid_rhyme(bad), "{bad} should not be a rhyme");
        }
    }

    #[test]
    fn binary_search_covers_the_whole_inventory() {
        // Both sorted views must agree with the source table for every entry.
        for rhyme in VALID_RHYMES {
            assert!(is_valid_rhyme(rhyme), "{rhyme} missing from sorted view");
            assert!(
                matches_deshaped(rhyme),
                "{rhyme} missing from deshaped view"
            );
        }
    }

    #[test]
    fn shaped_prefix_distinguishes_oe_from_nonexistent_o_circumflex_e() {
        assert!(has_prefix(&['o', 'e']));
        assert!(!has_prefix(&['ô', 'e']));
        assert!(has_prefix(&['â', 't']));
    }

    #[test]
    fn deshaped_match_is_whole_and_shape_blind() {
        // A rhyme the user has typed without its shape keys: `iên` spelled `ien`.
        for undiacriticked in ["ien", "uoc", "uong", "uu", "ue", "uyen", "ech", "yeu"] {
            assert!(!is_valid_rhyme(undiacriticked));
            assert!(matches_deshaped(undiacriticked), "{undiacriticked}");
        }
        // A *prefix* of one is not a match: `ie` is English `die`, not half a `diên`.
        for bad in ["ie", "ye", "uye", "ae", "uuy", "eg"] {
            assert!(!matches_deshaped(bad), "{bad}");
        }
        // Tone marks are stripped along with shape, so a toned rhyme still matches.
        assert!(matches_deshaped("iên"));
        assert!(matches_deshaped("ươc"));
    }
}
