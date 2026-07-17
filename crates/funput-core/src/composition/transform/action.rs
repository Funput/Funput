use crate::composition::apply::{
    apply_shape_key, apply_stroke, apply_tone_key, remove_tone, shape_apply_target_exists,
};
use crate::composition::intent::{ModifierIntent, resolve};
use crate::composition::revert::{try_revert_shape, try_revert_stroke, try_revert_tone};
use crate::composition::uo_horn::{ends_with_open_uo_horn, normalize_horned_uo_open};
use crate::input_method::KeyAction;
use crate::validation::syllable::{validate_shape, validate_stroke, validate_tone};
use crate::{ToneStyle, TransformKind, TransformResult};

use super::{append, gates, normal};

pub(crate) fn apply_action(
    buffer: &str,
    key: char,
    action: KeyAction,
    style: ToneStyle,
    spell_check: bool,
) -> TransformResult {
    match action {
        KeyAction::Stroke => stroke(buffer, key, spell_check),
        KeyAction::Tone(value) => tone(buffer, key, value, style, spell_check),
        KeyAction::Shape(value) => shape(buffer, key, value, spell_check),
        KeyAction::FreeCircumflex(stem) => {
            resolve(buffer, ModifierIntent::Circumflex { stem, key }, style).into_result()
        }
        KeyAction::DeferredW => {
            resolve(buffer, ModifierIntent::DeferredW { key }, style).into_result()
        }
        KeyAction::RemoveTone => {
            remove_tone(buffer).map_or_else(|| pending(append(buffer, key)), applied)
        }
        KeyAction::Normal => normal::apply(buffer, key, style),
    }
}

fn stroke(buffer: &str, key: char, checked: bool) -> TransformResult {
    if let Some(text) = try_revert_stroke(buffer) {
        return reverted(append(&text, key));
    }
    let result = gates::validation(buffer, key, validate_stroke(buffer))
        .unwrap_or_else(|| apply_stroke(buffer));
    gates::spell_check(buffer, key, checked, result)
}

fn tone(
    buffer: &str,
    key: char,
    tone: crate::unicode::marks::Tone,
    style: ToneStyle,
    checked: bool,
) -> TransformResult {
    let normalized = normalize_horned_uo_open(buffer);
    let buffer = normalized.as_deref().unwrap_or(buffer);
    if let Some(text) = try_revert_tone(buffer, tone, style) {
        return reverted(append(&text, key));
    }
    let result = gates::validation(buffer, key, validate_tone(buffer))
        .unwrap_or_else(|| apply_tone_key(buffer, tone, style));
    gates::spell_check(buffer, key, checked, result)
}

fn shape(
    buffer: &str,
    key: char,
    shape: crate::unicode::shapes::VowelShape,
    checked: bool,
) -> TransformResult {
    if shape == crate::unicode::shapes::VowelShape::Horn
        && ends_with_open_uo_horn(buffer)
        && let Some(text) = try_revert_shape(buffer, shape)
    {
        return reverted(append(&text, key));
    }
    if shape_apply_target_exists(buffer, shape) {
        let result = gates::validation(buffer, key, validate_shape(buffer))
            .unwrap_or_else(|| apply_shape_key(buffer, shape));
        return gates::spell_check(buffer, key, checked, result);
    }
    if let Some(text) = try_revert_shape(buffer, shape) {
        return reverted(append(&text, key));
    }
    let result = gates::validation(buffer, key, validate_shape(buffer))
        .unwrap_or_else(|| apply_shape_key(buffer, shape));
    gates::spell_check(buffer, key, checked, result)
}

fn applied(text: String) -> TransformResult {
    TransformResult {
        kind: TransformKind::Applied,
        text,
    }
}

fn pending(text: String) -> TransformResult {
    TransformResult {
        kind: TransformKind::Pending,
        text,
    }
}

fn reverted(text: String) -> TransformResult {
    TransformResult {
        kind: TransformKind::Reverted,
        text,
    }
}
