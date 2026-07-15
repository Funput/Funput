//! Per-action buffer transforms (stroke, tone, shape).
//!
//! Method-agnostic: these operate on a [`crate::input_method::KeyAction`] already
//! resolved by a classifier, so VNI and Telex share them unchanged. Hot path:
//! each transform builds its result `String` in one pass — no intermediate
//! `Vec<char>` collections.

use crate::composition::replace_char_at;
use crate::composition::uo_horn::{apply_uo_compound, uo_pair_in_vowel_cluster};
use crate::unicode::marks::{Tone, apply_tone_to_vowel, stroke_d, tone_on_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, apply_shape_to_vowel, shape_target_index};
use crate::unicode::tone_position::{tone_target_vowel, tone_vowel_index};
use crate::{ToneStyle, TransformKind, TransformResult};

fn ignored(buffer: &str) -> TransformResult {
    TransformResult {
        kind: TransformKind::Ignored,
        text: buffer.to_owned(),
    }
}

fn applied(text: String) -> TransformResult {
    TransformResult {
        kind: TransformKind::Applied,
        text,
    }
}

/// Turn a `d`/`D` into `đ`/`Đ` so the key works wherever it is typed: `dang` + `9`
/// → `đang`, not only `d` + `9`. Targets the **last** `d` in the buffer — a
/// Vietnamese syllable has at most one `d` (always the onset), and in an
/// abbreviation run the last one is the most recent onset (`GD` + `9` → `GĐ`,
/// `GDD` → `GĐ`).
pub(crate) fn apply_stroke(buffer: &str) -> TransformResult {
    let Some((offset, d)) = buffer
        .char_indices()
        .rev()
        .find(|&(_, c)| matches!(c, 'd' | 'D'))
    else {
        return ignored(buffer);
    };
    let Some(struck) = stroke_d(d) else {
        return ignored(buffer);
    };

    let mut text = String::with_capacity(buffer.len() + struck.len_utf8());
    text.push_str(&buffer[..offset]);
    text.push(struck);
    text.push_str(&buffer[offset + d.len_utf8()..]);
    applied(text)
}

/// Place `tone` on the nucleus vowel (handles reposition and `ie`/`gie` → `ê`).
pub(crate) fn apply_tone_key(buffer: &str, tone: Tone, style: ToneStyle) -> TransformResult {
    let Some(vowel_idx) = tone_vowel_index(buffer, style) else {
        return ignored(buffer);
    };

    let Some(vowel) = buffer.chars().nth(vowel_idx) else {
        return ignored(buffer);
    };
    let tone_target = tone_target_vowel(buffer, vowel_idx).unwrap_or(vowel);
    let Some(toned) = apply_tone_to_vowel(tone_target, tone) else {
        return ignored(buffer);
    };

    applied(replace_char_at(buffer, vowel_idx, toned))
}

/// Remove the tone mark from the syllable, keeping any shape (`việt` → `viêt`,
/// `toán` → `toan`, `được` → `đươc`). Returns `None` when there is no tone.
pub(crate) fn remove_tone(buffer: &str) -> Option<String> {
    let (idx, stem) = buffer.chars().enumerate().find_map(|(i, ch)| {
        tone_on_vowel(ch)?;
        Some((i, vowel_stem(ch)?)) // strips tone, keeps shape
    })?;
    Some(replace_char_at(buffer, idx, stem))
}

/// True if `shape` can still be applied to some vowel in `buffer` (the horn
/// `uo` compound, or a single vowel that can receive the shape).
pub(crate) fn shape_apply_target_exists(buffer: &str, shape: VowelShape) -> bool {
    if shape == VowelShape::Horn && uo_pair_in_vowel_cluster(buffer).is_some() {
        return true;
    }
    shape_target_index(buffer, shape).is_some()
}

/// Apply a vowel shape. For an adjacent plain `uo`, initially horn only the `o`
/// (`uơ`): that is the completed open rhyme in `thuở`/`huơ`/`quơ`. If another
/// vowel or coda arrives, [`crate::composition::uo_horn::complete_uo_horn_for_continuation`]
/// turns it into `ươ` (`thương`, `hươu`).
pub(crate) fn apply_shape_key(buffer: &str, shape: VowelShape) -> TransformResult {
    if shape == VowelShape::Horn
        && let Some(text) = apply_uo_compound(buffer)
    {
        return applied(text);
    }

    let Some(vowel_idx) = shape_target_index(buffer, shape) else {
        return ignored(buffer);
    };

    let Some(vowel) = buffer.chars().nth(vowel_idx) else {
        return ignored(buffer);
    };
    let Some(shaped) = apply_shape_to_vowel(vowel, shape) else {
        return ignored(buffer);
    };

    // Carry any tone already on the vowel across the shape change — `apply_shape_to_vowel`
    // drops it (it shapes the bare stem). This makes the mark order free: shaping a
    // toned vowel keeps the tone (`asw` → `ắ`, not `ă`; `uasw` → `ứa`), matching the
    // usual shape-then-tone order and VNI's position-free modifiers.
    let shaped = match tone_on_vowel(vowel) {
        Some(tone) => apply_tone_to_vowel(shaped, tone).unwrap_or(shaped),
        None => shaped,
    };

    applied(replace_char_at(buffer, vowel_idx, shaped))
}
