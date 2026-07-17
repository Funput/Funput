//! Targeted modifier intents used only when an input method's adjacent fast path
//! cannot resolve a key.

mod candidate;
mod circumflex;
mod target;

use crate::input_method::CircumflexStem;
use crate::{ToneStyle, TransformKind, TransformResult};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum ModifierIntent {
    Circumflex { stem: CircumflexStem, key: char },
}

#[derive(Debug, PartialEq, Eq)]
pub(crate) enum IntentResolution {
    Applied(String),
    Reverted(String),
    #[expect(dead_code, reason = "reserved for the deferred-w V2 client")]
    Deferred(String),
    Literal(String),
}

pub(crate) fn resolve(buffer: &str, intent: ModifierIntent, style: ToneStyle) -> IntentResolution {
    match intent {
        ModifierIntent::Circumflex { stem, key } => {
            circumflex::resolve(buffer, char::from(stem), key, style)
        }
    }
}

impl IntentResolution {
    pub(crate) fn into_result(self) -> TransformResult {
        let (kind, text) = match self {
            Self::Applied(text) => (TransformKind::Applied, text),
            Self::Reverted(text) => (TransformKind::Reverted, text),
            Self::Deferred(text) | Self::Literal(text) => (TransformKind::Pending, text),
        };
        TransformResult { kind, text }
    }
}

#[cfg(test)]
mod tests;
