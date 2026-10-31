use crate::input_method::KeyAction;
use crate::unicode::shapes::VowelShape;

use super::super::{classify_key, telex_defers_w};

fn shape(buffer: &str, expected: VowelShape) {
    assert_eq!(classify_key(buffer, 'w'), KeyAction::Shape(expected));
}

#[test]
fn adjacent_rules() {
    shape("a", VowelShape::Breve);
    shape("o", VowelShape::Horn);
    shape("u", VowelShape::Horn);
    shape("uo", VowelShape::Horn);
    shape("ơ", VowelShape::Horn);
}

#[test]
fn free_position_rules() {
    shape("lam", VowelShape::Breve);
    shape("con", VowelShape::Horn);
    shape("nuoc", VowelShape::Horn);
    shape("moi", VowelShape::Horn);
    shape("oa", VowelShape::Breve);
    assert_eq!(classify_key("eng", 'w'), KeyAction::Normal);
    assert_eq!(classify_key("ng", 'w'), KeyAction::Normal);
}

#[test]
fn ua_targets_u_except_qu_glide() {
    for buffer in ["nua", "mua", "ngua", "úa", "nưa"] {
        shape(buffer, VowelShape::Horn);
    }
    shape("qua", VowelShape::Breve);
    shape("hoa", VowelShape::Breve);
}

#[test]
fn defers_one_w_typed_between_onset_and_nucleus() {
    for keys in ["two", "swan", "swift", "cwon", "nwux", "quwon", "Two", "tw"] {
        assert!(telex_defers_w(keys), "{keys}");
    }
    // Leading, after the nucleus, or not alone before it: no deferred modifier.
    for keys in ["tow", "w", "was", "lww", "law", "t", ""] {
        assert!(!telex_defers_w(keys), "{keys}");
    }
}
