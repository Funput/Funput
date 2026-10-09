use crate::composition::apply::{
    apply_shape_key, apply_stroke, apply_tone_key, remove_tone, shape_apply_target_exists,
};
use crate::composition::intent::{ModifierIntent, resolve};
use crate::composition::revert::{
    try_revert_own_circumflex, try_revert_shape, try_revert_stroke, try_revert_tone,
};
use crate::composition::uo_horn::{ends_with_open_uo_horn, normalize_horned_uo_open};
use crate::input_method::KeyAction;
use crate::validation::syllable::{validate_shape, validate_stroke, validate_tone};
use crate::{ComposeOptions, TransformKind, TransformResult};

use super::{append, gates, normal};

pub(crate) fn apply_action(
    buffer: &str,
    key: char,
    action: KeyAction,
    options: ComposeOptions,
) -> TransformResult {
    match action {
        KeyAction::Stroke => stroke(buffer, key, options),
        KeyAction::Tone(value) => tone(buffer, key, value, options),
        KeyAction::Shape(value) => shape(buffer, key, value, options),
        KeyAction::FreeCircumflex(stem) => {
            resolve(buffer, ModifierIntent::Circumflex { stem, key }, options).into_result()
        }
        KeyAction::DeferredW => {
            resolve(buffer, ModifierIntent::DeferredW { key }, options).into_result()
        }
        KeyAction::RemoveTone => {
            remove_tone(buffer).map_or_else(|| pending(append(buffer, key)), applied)
        }
        KeyAction::Normal => normal::apply(buffer, key, options.tone_style),
    }
}

fn stroke(buffer: &str, key: char, options: ComposeOptions) -> TransformResult {
    if !ends_with_stroke_target(buffer) {
        let result = resolve(buffer, ModifierIntent::Stroke { key }, options).into_result();
        return gates::spell_check(buffer, key, options, result);
    }
    if let Some(text) = try_revert_stroke(buffer) {
        return reverted(append(&text, key));
    }
    let result = gates::validation(buffer, key, validate_stroke(buffer))
        .unwrap_or_else(|| apply_stroke(buffer));
    gates::spell_check(buffer, key, options, result)
}

fn ends_with_stroke_target(buffer: &str) -> bool {
    buffer
        .chars()
        .last()
        .is_some_and(|ch| matches!(ch, 'd' | 'D' | 'đ' | 'Đ'))
}

fn tone(
    buffer: &str,
    key: char,
    tone: crate::unicode::marks::Tone,
    options: ComposeOptions,
) -> TransformResult {
    let style = options.tone_style;
    let normalized = normalize_horned_uo_open(buffer);
    let buffer = normalized.as_deref().unwrap_or(buffer);
    if let Some(text) = try_revert_tone(buffer, tone, style) {
        return reverted(append(&text, key));
    }
    let result = gates::validation(buffer, key, validate_tone(buffer, options.syllable_rules))
        .unwrap_or_else(|| apply_tone_key(buffer, tone, style));
    gates::spell_check(buffer, key, options, result)
}

fn shape(
    buffer: &str,
    key: char,
    shape: crate::unicode::shapes::VowelShape,
    options: ComposeOptions,
) -> TransformResult {
    if shape == crate::unicode::shapes::VowelShape::Horn
        && ends_with_open_uo_horn(buffer)
        && let Some(text) = try_revert_shape(buffer, shape)
    {
        return reverted(append(&text, key));
    }
    if shape == crate::unicode::shapes::VowelShape::Circumflex
        && let Some(text) = try_revert_own_circumflex(buffer, key)
    {
        return reverted(append(&text, key));
    }
    if shape_apply_target_exists(buffer, shape) {
        let result = gates::validation(buffer, key, validate_shape(buffer, options.syllable_rules))
            .unwrap_or_else(|| apply_shape_key(buffer, shape));
        return gates::spell_check(buffer, key, options, result);
    }
    if let Some(text) = try_revert_shape(buffer, shape) {
        return reverted(append(&text, key));
    }
    let result = gates::validation(buffer, key, validate_shape(buffer, options.syllable_rules))
        .unwrap_or_else(|| apply_shape_key(buffer, shape));
    gates::spell_check(buffer, key, options, result)
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
