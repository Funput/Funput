use crate::validation::reachability::is_definitely_invalid;
use crate::validation::syllable::ModifierValidation;
use crate::{TransformKind, TransformResult};

use super::append;

pub(super) fn spell_check(
    buffer: &str,
    key: char,
    enabled: bool,
    result: TransformResult,
) -> TransformResult {
    if enabled && result.kind == TransformKind::Applied && is_definitely_invalid(&result.text) {
        return TransformResult {
            kind: TransformKind::Pending,
            text: append(buffer, key),
        };
    }
    result
}

pub(super) fn validation(
    buffer: &str,
    key: char,
    result: ModifierValidation,
) -> Option<TransformResult> {
    let kind = match result {
        ModifierValidation::Allow => return None,
        ModifierValidation::Ignored => TransformKind::Ignored,
        ModifierValidation::PassThrough => TransformKind::Pending,
    };
    let text = if kind == TransformKind::Ignored {
        buffer.to_owned()
    } else {
        append(buffer, key)
    };
    Some(TransformResult { kind, text })
}
