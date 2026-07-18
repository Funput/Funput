use funput_core::ToneStyle;

use super::super::sim::{SimConfig, simulate_with};
use super::*;

fn roundtrip(word: &str, method: InputMethod) -> String {
    let keys = encode(word, method);
    let config = SimConfig {
        method,
        tone_style: ToneStyle::Traditional,
        smart_restore: false,
        spell_check: false,
    };
    simulate_with(config, &keys).app_text
}

const WORDS: &[&str] = &[
    "đầu", "việt", "nước", "Đắk", "nam", "tiếng", "người", "được", "rượu", "nghiêng", "Ô", "khuỷu",
    "boong", "xoong", "soóc", "moóc", "voọc", "coong",
];

#[test]
fn telex_family_roundtrip() {
    for method in [InputMethod::Telex, InputMethod::TelexAdvanced] {
        for &word in WORDS {
            assert_eq!(roundtrip(word, method), word, "{method:?}: {word}");
        }
    }
}

#[test]
fn advanced_encoder_exercises_full_telex_shortcuts() {
    for (word, keys) in [
        ("ư", "w"),
        ("Ư", "W"),
        ("ứng", "wsng"),
        ("tư", "t["),
        ("mơ", "m]"),
        ("trường", "tr[]fng"),
        ("người", "ng[]f i"),
    ] {
        let encoded = encode(word, InputMethod::TelexAdvanced);
        let expected = keys.replace(' ', "");
        assert_eq!(encoded, expected, "{word}");
        assert_eq!(roundtrip(word, InputMethod::TelexAdvanced), word, "{word}");
    }
}

#[test]
fn vni_roundtrip() {
    for &word in WORDS {
        assert_eq!(roundtrip(word, InputMethod::Vni), word, "vni: {word}");
    }
}
