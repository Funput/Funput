//! Curated Telex golden corpus. Each `keys<TAB>expected` line must compose exactly.
//! The set deliberately covers the tricky rhymes that are easy to regress (glides,
//! iê/yê/uyê, ươ, ă/â + coda, oo/ôô loanwords, case, literal pass-through).
//!
//! Tone style is **Traditional** (matches `support::type_keys`), so expected values
//! use the old placement (`hòa`, `khỏe`, `thúy`). Comment lines start with `#`.

use funput_core::InputMethod;

use crate::support::type_keys;

const CORPUS: &str = include_str!("data/telex_corpus.tsv");

#[test]
fn telex_corpus_composes_exactly() {
    let mut checked = 0usize;
    for (index, raw) in CORPUS.lines().enumerate() {
        let line = raw.trim_end();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        let (keys, expected) = line
            .split_once('\t')
            .unwrap_or_else(|| panic!("line {}: missing TAB separator: {raw:?}", index + 1));
        assert_eq!(
            type_keys(InputMethod::Telex, keys),
            expected,
            "line {}: keys {keys:?}",
            index + 1
        );
        checked += 1;
    }
    assert!(checked >= 100, "corpus unexpectedly small: {checked} cases");
}
