use crate::composition::uo_horn::complete_uo_horn_for_continuation;
use crate::unicode::tone_position::reposition_existing_tone;
use crate::{ToneStyle, TransformKind, TransformResult};

use super::append;

pub(super) fn apply(buffer: &str, key: char, style: ToneStyle) -> TransformResult {
    if let Some(completed) = complete_uo_horn_for_continuation(buffer, key) {
        return repositioned_or(completed, style, TransformKind::Applied);
    }
    repositioned_or(append(buffer, key), style, TransformKind::Pending)
}

fn repositioned_or(text: String, style: ToneStyle, fallback: TransformKind) -> TransformResult {
    match reposition_existing_tone(&text, style) {
        Some(text) => TransformResult {
            kind: TransformKind::Applied,
            text,
        },
        None => TransformResult {
            kind: fallback,
            text,
        },
    }
}
