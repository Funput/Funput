//! `respell_tone`, the public face of tone repositioning for finished words.

use crate::ToneStyle::{Modern, Traditional};
use crate::respell_tone;

#[test]
fn an_open_glide_syllable_moves_its_tone_between_styles() {
    for (modern, traditional) in [
        ("hoà", "hòa"),
        ("khoẻ", "khỏe"),
        ("thuỷ", "thủy"),
        ("loè", "lòe"),
        ("uỷ", "ủy"),
    ] {
        assert_eq!(
            respell_tone(modern, Traditional).as_deref(),
            Some(traditional)
        );
        assert_eq!(respell_tone(traditional, Modern).as_deref(), Some(modern));
        assert_eq!(
            respell_tone(modern, Modern),
            None,
            "{modern} is already modern"
        );
        assert_eq!(respell_tone(traditional, Traditional), None);
    }
}

#[test]
fn a_syllable_both_styles_spell_alike_is_left_alone() {
    for word in [
        "hoàng", "quý", "thuế", "ơn", "cảm", "loẹt", "hoa", "giữa", "",
    ] {
        assert_eq!(respell_tone(word, Traditional), None, "{word}");
        assert_eq!(respell_tone(word, Modern), None, "{word}");
    }
}
