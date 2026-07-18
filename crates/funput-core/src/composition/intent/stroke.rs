use crate::TransformKind;
use crate::composition::apply::apply_stroke;
use crate::composition::revert::try_revert_stroke;

use super::IntentResolution;

pub(super) fn resolve(buffer: &str, key: char) -> IntentResolution {
    if let Some(mut text) = try_revert_stroke(buffer) {
        text.push(key);
        return IntentResolution::Reverted(text);
    }

    let result = apply_stroke(buffer);
    if result.kind == TransformKind::Applied {
        IntentResolution::Applied(result.text)
    } else {
        let mut text = result.text;
        text.push(key);
        IntentResolution::Literal(text)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn applies_and_reverts_the_existing_target() {
        assert_eq!(
            resolve("duoc", 'd'),
            IntentResolution::Applied("đuoc".into())
        );
        assert_eq!(
            resolve("được", 'd'),
            IntentResolution::Reverted("dượcd".into())
        );
        assert_eq!(
            resolve("DUOC", 'D'),
            IntentResolution::Applied("ĐUOC".into())
        );
    }
}
