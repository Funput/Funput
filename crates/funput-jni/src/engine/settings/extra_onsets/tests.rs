use funput_core::{ExtraOnsets, InputMethod, ToneStyle};
use funput_engine::EngineConfig;

use super::{decode, set_extra_onsets};
use crate::engine::registry;

#[test]
fn wire_bits_map_individually_and_ignore_unknown_bits() {
    for (flag, onset) in [
        (1, ExtraOnsets::F),
        (2, ExtraOnsets::J),
        (4, ExtraOnsets::W),
        (8, ExtraOnsets::Z),
    ] {
        assert_eq!(decode(flag), onset);
        assert_eq!(decode(flag | 0x100), onset);
    }
    assert_eq!(decode(0), ExtraOnsets::NONE);
    assert_eq!(decode(0x100), ExtraOnsets::NONE);
    assert_eq!(decode(15), ExtraOnsets::ZFWJ);
}

#[test]
fn each_selection_only_admits_its_own_onset() {
    let cases = [
        (8, "zoo", "zô"),
        (1, "fair", "fải"),
        (4, "was", "wá"),
        (2, "jowf", "jờ"),
    ];
    for (flag, keys, expected) in cases {
        assert_eq!(type_with(InputMethod::Telex, flag, keys), expected);
        assert_eq!(type_with(InputMethod::Telex, 0, keys), keys);
        for other in [1, 2, 4, 8].into_iter().filter(|other| *other != flag) {
            assert_eq!(type_with(InputMethod::Telex, other, keys), keys);
        }
    }
}

#[test]
fn vni_and_advanced_telex_use_the_same_spelling_rules() {
    assert_eq!(type_with(InputMethod::Vni, 8, "zo6"), "zô");
    assert_eq!(type_with(InputMethod::Vni, 2, "jo72"), "jờ");
    assert_eq!(type_with(InputMethod::TelexAdvanced, 4, "wa"), "ưa");
    assert_eq!(type_with(InputMethod::TelexAdvanced, 4, "wwas"), "wá");
    assert_eq!(type_with(InputMethod::TelexAdvanced, 1, "fair"), "fải");
}

#[test]
fn english_tradeoff_and_escape_are_unchanged() {
    assert_eq!(type_with(InputMethod::Telex, 15, "food"), "food");
    assert_eq!(type_with(InputMethod::Telex, 1, "fast"), "fát");
    assert_eq!(type_with(InputMethod::Telex, 1, "fasst"), "fast");
}

#[test]
fn setter_preserves_other_options_and_can_turn_every_letter_off() {
    let handle = registry::create();
    let original = EngineConfig {
        method: InputMethod::Vni,
        tone_style: ToneStyle::Traditional,
        smart_restore: false,
        eager_restore: false,
        spell_check: true,
        auto_capitalize: true,
        shortcuts_enabled: false,
        shortcut_smart_case: false,
        shortcuts_in_english: false,
        ..EngineConfig::default()
    };
    registry::with_mut(handle, |engine| engine.configure(original.clone()));
    set_extra_onsets(handle, 15);
    registry::with_mut(handle, |engine| {
        let mut expected = original.clone();
        expected.syllable_rules = expected.syllable_rules.with_extra_onsets(ExtraOnsets::ZFWJ);
        assert_eq!(engine.config(), &expected);
    });
    set_extra_onsets(handle, 0);
    registry::with_mut(handle, |engine| assert_eq!(engine.config(), &original));
    registry::destroy(handle);
}

#[test]
fn unknown_and_destroyed_handles_are_safe() {
    set_extra_onsets(0, 15);
    set_extra_onsets(-1, 15);
    let handle = registry::create();
    registry::destroy(handle);
    set_extra_onsets(handle, 15);
    assert!(registry::with_mut(handle, |engine| engine.is_enabled()).is_none());
}

fn type_with(method: InputMethod, letters: i32, keys: &str) -> String {
    let handle = registry::create();
    registry::with_mut(handle, |engine| engine.set_method(method));
    set_extra_onsets(handle, letters);
    let output = registry::with_mut(handle, |engine| {
        for key in keys.chars() {
            engine.process_char(key);
        }
        engine.buffer().to_owned()
    })
    .expect("registered engine");
    registry::destroy(handle);
    output
}
