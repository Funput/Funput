//! Moving a tone mark between the two tone styles.
//!
//! The styles only disagree on an open syllable ending in `oa`, `oe` or `uy`: the
//! traditional style puts the tone on the first vowel (`hòa`, `khỏe`, `thủy`), the
//! modern one on the second (`hoà`, `khoẻ`, `thuỷ`). A final consonant settles it
//! the same way in both (`hoàng`), and so does `qu`, whose `u` is part of the onset
//! (`quý`). The shipped lists are written in the modern style; this gives the
//! other spelling, once, when a table is built — never on the query path.

use funput_core::ToneStyle;
use unicode_normalization::UnicodeNormalization;

/// The five tone marks, as NFD writes them.
const TONES: [char; 5] = ['\u{0300}', '\u{0301}', '\u{0303}', '\u{0309}', '\u{0323}'];

const PAIRS: [[char; 2]; 3] = [['o', 'a'], ['o', 'e'], ['u', 'y']];

/// `word`, a lowercase NFC syllable, spelled in `style`, or `None` when both
/// styles spell it the same way.
pub(crate) fn respell(word: &str, style: ToneStyle) -> Option<String> {
    let decomposed: Vec<char> = word.nfd().collect();
    let tone = decomposed.iter().copied().find(|c| TONES.contains(c))?;
    let base: Vec<char> = decomposed.into_iter().filter(|c| *c != tone).collect();
    // Any other mark (`ê`, `ă`) puts the tone on that vowel in both styles.
    if base.iter().any(|c| ('\u{0300}'..='\u{036F}').contains(c)) {
        return None;
    }
    let n = base.len();
    let ending = [*base.get(n.wrapping_sub(2))?, *base.last()?];
    if !PAIRS.contains(&ending) || (ending[0] == 'u' && n >= 3 && base[n - 3] == 'q') {
        return None;
    }
    let at = match style {
        ToneStyle::Traditional => n - 1,
        ToneStyle::Modern => n,
    };
    let mut chars = base;
    chars.insert(at, tone);
    let spelled: String = chars.into_iter().nfc().collect();
    (spelled != word).then_some(spelled)
}

#[cfg(test)]
mod tests {
    use funput_core::ToneStyle::{Modern, Traditional};

    use super::respell;

    #[test]
    fn an_open_glide_syllable_moves_its_tone() {
        assert_eq!(respell("hoà", Traditional).as_deref(), Some("hòa"));
        assert_eq!(respell("khoẻ", Traditional).as_deref(), Some("khỏe"));
        assert_eq!(respell("thuỷ", Traditional).as_deref(), Some("thủy"));
        assert_eq!(respell("uỷ", Traditional).as_deref(), Some("ủy"));
        assert_eq!(respell("hòa", Modern).as_deref(), Some("hoà"));
        assert_eq!(respell("loè", Traditional).as_deref(), Some("lòe"));
    }

    #[test]
    fn a_syllable_both_styles_agree_on_is_left_alone() {
        for word in ["hoà", "khoẻ", "thuỷ"] {
            assert_eq!(respell(word, Modern), None, "{word} is already modern");
        }
        for word in [
            "hoàng", "quý", "thuế", "khuya", "ơn", "cảm", "loẹt", "hoa", "",
        ] {
            assert_eq!(respell(word, Traditional), None, "{word}");
            assert_eq!(respell(word, Modern), None, "{word}");
        }
    }

    /// `data/syllables/vi.txt` lists both spellings of every syllable the styles
    /// disagree on, so it is an independent account of the rule above.
    #[test]
    fn every_dual_spelling_on_the_syllable_list_is_one_respelling_apart() {
        let list = include_str!("../../data/syllables/vi.txt");
        let words: std::collections::BTreeSet<&str> = list
            .lines()
            .map(str::trim)
            .filter(|line| !line.is_empty() && !line.starts_with('#'))
            .collect();
        let mut checked = 0;
        for word in &words {
            if let Some(other) = respell(word, Traditional) {
                assert!(words.contains(other.as_str()), "{word} → {other}");
                assert_eq!(respell(&other, Modern).as_deref(), Some(*word));
                checked += 1;
            }
        }
        assert!(
            checked >= 10,
            "only {checked} dual spellings were exercised"
        );
    }
}
