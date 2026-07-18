//! Transform pipeline: classify a key, then resolve the selected action.

mod action;
mod full_telex;
mod gates;
mod normal;

use crate::input_method::{AdvancedAction, telex, vni};
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

pub(crate) fn apply_advanced_telex(
    buffer: &str,
    key: char,
    style: ToneStyle,
    spell_check: bool,
) -> TransformResult {
    match telex::classify_advanced_key(buffer, key) {
        AdvancedAction::Standard(action) => apply_action(buffer, key, action, style, spell_check),
        AdvancedAction::Shortcut(shortcut) => full_telex::apply(buffer, key, shortcut, spell_check),
    }
}

pub(super) fn append(buffer: &str, key: char) -> String {
    let mut text = String::with_capacity(buffer.len() + key.len_utf8());
    text.push_str(buffer);
    text.push(key);
    text
}

#[cfg(test)]
mod tests;
