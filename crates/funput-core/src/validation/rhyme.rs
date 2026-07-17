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

/// Valid toneless rhymes (lowercase, shaped vowels: `ươ`, `iê`, …).
const VALID_RHYMES: &[&str] = &[
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
static SORTED_RHYMES: std::sync::LazyLock<Vec<&'static str>> = std::sync::LazyLock::new(|| {
    let mut sorted = VALID_RHYMES.to_vec();
    sorted.sort_unstable();
    sorted
});

/// True if `rhyme` (toneless, lowercase) is a valid Vietnamese rhyme.
pub fn is_valid_rhyme(rhyme: &str) -> bool {
    SORTED_RHYMES.binary_search(&rhyme).is_ok()
}

/// Whether any valid shaped rhyme starts with `prefix`.
///
/// Used by free-position shape candidates, where deshaping would over-accept an
/// impossible rhyme such as `ôe` merely because plain `oe` exists.
pub(crate) fn has_prefix(prefix: &[char]) -> bool {
    let first = SORTED_RHYMES.partition_point(|rhyme| {
        rhyme.chars().cmp(prefix.iter().copied()) == std::cmp::Ordering::Less
    });
    SORTED_RHYMES.get(first).is_some_and(|rhyme| {
        let mut chars = rhyme.chars();
        prefix
            .iter()
            .all(|&expected| chars.next() == Some(expected))
    })
}

/// The full toneless rhyme inventory (for prefix/reachability checks).
pub fn all() -> &'static [&'static str] {
    VALID_RHYMES
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
        // The sorted view must agree with the source table for every entry.
        for rhyme in all() {
            assert!(is_valid_rhyme(rhyme), "{rhyme} missing from sorted view");
        }
    }

    #[test]
    fn shaped_prefix_distinguishes_oe_from_nonexistent_o_circumflex_e() {
        assert!(has_prefix(&['o', 'e']));
        assert!(!has_prefix(&['ô', 'e']));
        assert!(has_prefix(&['â', 't']));
    }
}
