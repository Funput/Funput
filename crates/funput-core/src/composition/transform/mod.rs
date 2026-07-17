//! Transform pipeline: classify a key, then resolve the selected action.

mod action;
mod gates;
mod normal;

use crate::input_method::{telex, vni};
use crate::{ToneStyle, TransformResult};

pub(crate) use action::apply_action;

pub(crate) fn apply_vni(
    buffer: &str,
    key: char,
    style: ToneStyle,
    spell_check: bool,
) -> TransformResult {
    apply_action(
        buffer,
        key,
        vni::classify_key(buffer, key),
        style,
        spell_check,
    )
}

pub(crate) fn apply_telex(
    buffer: &str,
    key: char,
    style: ToneStyle,
    spell_check: bool,
) -> TransformResult {
    apply_action(
        buffer,
        key,
        telex::classify_key(buffer, key),
        style,
        spell_check,
    )
}

pub(super) fn append(buffer: &str, key: char) -> String {
    let mut text = String::with_capacity(buffer.len() + key.len_utf8());
    text.push_str(buffer);
    text.push(key);
    text
}

#[cfg(test)]
mod tests;
