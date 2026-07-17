use crate::input_method::{CircumflexStem, KeyAction};
use crate::unicode::marks::Tone;
use crate::unicode::shapes::VowelShape;

use super::super::classify_key;

#[test]
fn tones_and_literal_letters() {
    for (key, tone) in [
        ('s', Tone::Sac),
        ('f', Tone::Huyen),
        ('r', Tone::Hoi),
        ('x', Tone::Nga),
        ('j', Tone::Nang),
    ] {
        assert_eq!(classify_key("a", key), KeyAction::Tone(tone));
    }
    assert_eq!(classify_key("ng", 's'), KeyAction::Normal);
    assert_eq!(classify_key("", 'f'), KeyAction::Normal);
    assert_eq!(classify_key("a", 'm'), KeyAction::Normal);
}

#[test]
fn stroke_and_remove() {
    assert_eq!(classify_key("d", 'd'), KeyAction::Stroke);
    assert_eq!(classify_key("duoc", 'd'), KeyAction::Stroke);
    assert_eq!(classify_key("tao", 'd'), KeyAction::Normal);
    assert_eq!(classify_key("á", 'z'), KeyAction::RemoveTone);
}

#[test]
fn adjacent_and_free_circumflex() {
    assert_eq!(
        classify_key("a", 'a'),
        KeyAction::Shape(VowelShape::Circumflex)
    );
    assert_eq!(
        classify_key("â", 'a'),
        KeyAction::Shape(VowelShape::Circumflex)
    );
    assert_eq!(
        classify_key("chan", 'a'),
        KeyAction::FreeCircumflex(CircumflexStem::A)
    );
    assert_eq!(
        classify_key("dèm", 'e'),
        KeyAction::FreeCircumflex(CircumflexStem::E)
    );
    assert_eq!(classify_key("cha n", 'a'), KeyAction::Normal);
}

#[test]
fn action_stays_compact() {
    assert!(std::mem::size_of::<KeyAction>() <= 2);
}
