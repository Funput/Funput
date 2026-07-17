use crate::ToneStyle;
use crate::input_method::CircumflexStem;

use super::{IntentResolution, ModifierIntent, resolve};

fn apply(buffer: &str, key: char) -> IntentResolution {
    let stem = CircumflexStem::from_key(key).expect("circumflex stem");
    resolve(
        buffer,
        ModifierIntent::Circumflex { stem, key },
        ToneStyle::Traditional,
    )
}

#[test]
fn targeted_apply_case_tone_and_fallback() {
    assert_eq!(apply("chan", 'a'), IntentResolution::Applied("chân".into()));
    assert_eq!(apply("chàn", 'a'), IntentResolution::Applied("chần".into()));
    assert_eq!(apply("ChAn", 'A'), IntentResolution::Applied("ChÂn".into()));
    assert_eq!(
        apply("camer", 'a'),
        IntentResolution::Literal("camera".into())
    );
    assert_eq!(apply("oao", 'o'), IntentResolution::Literal("oaoo".into()));
}

#[test]
fn early_tone_and_non_adjacent_revert() {
    assert_eq!(
        apply("chfan", 'a'),
        IntentResolution::Applied("chần".into())
    );
    assert_eq!(
        apply("chân", 'a'),
        IntentResolution::Reverted("chana".into())
    );
}
