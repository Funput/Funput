use crate::validation::syllable::ModifierValidation;
use crate::{ComposeOptions, TransformKind, TransformResult};

use super::append;

/// The spell-check gate: an applied diacritic that leaves no way to a Vietnamese
/// syllable is undone, and the key goes in as a literal instead.
pub(super) fn spell_check(
    buffer: &str,
    key: char,
    options: ComposeOptions,
    result: TransformResult,
) -> TransformResult {
    if options.spell_check
        && result.kind == TransformKind::Applied
        && options.syllable_rules.is_definitely_invalid(&result.text)
    {
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
