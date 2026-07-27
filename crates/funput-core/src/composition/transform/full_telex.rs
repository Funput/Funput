use crate::input_method::TelexShortcut;
use crate::{TransformKind, TransformResult};

use super::gates;

pub(super) fn apply(
    buffer: &str,
    key: char,
    shortcut: TelexShortcut,
    spell_check: bool,
) -> TransformResult {
    if shortcut == TelexShortcut::RepeatedW {
        return TransformResult {
            kind: TransformKind::Reverted,
            text: buffer.to_owned(),
        };
    }
    // `w` on the `ư` a leading `w` produced puts the literal key back, keeping the
    // onset in front of it: `ư` → `w`, `thư` → `thw`, `sư` → `sw`.
    if shortcut == TelexShortcut::LeadingW
        && let Some((prefix, horn_u)) = split_trailing_horn_u(buffer)
    {
        let mut text = String::with_capacity(prefix.len() + 1);
        text.push_str(prefix);
        text.push(if horn_u == 'Ư' { 'W' } else { 'w' });
        return TransformResult {
            kind: TransformKind::Reverted,
            text,
        };
    }

    let replacement = match shortcut {
        TelexShortcut::LeadingW if key == 'W' => 'Ư',
        TelexShortcut::LeadingW | TelexShortcut::HornU => 'ư',
        TelexShortcut::HornO => 'ơ',
        TelexShortcut::RepeatedW => unreachable!("handled above"),
    };
    let mut text = String::with_capacity(buffer.len() + replacement.len_utf8());
    text.push_str(buffer);
    text.push(replacement);
    let result = TransformResult {
        kind: TransformKind::Applied,
        text,
    };
    gates::spell_check(buffer, key, spell_check, result)
}

/// Split a buffer that ends in `ư`/`Ư` into the part before it and that vowel.
fn split_trailing_horn_u(buffer: &str) -> Option<(&str, char)> {
    let horn_u = buffer
        .chars()
        .next_back()
        .filter(|c| matches!(c, 'ư' | 'Ư'))?;
    Some((&buffer[..buffer.len() - horn_u.len_utf8()], horn_u))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shortcut_applies_and_leading_w_reverts() {
        assert_eq!(apply("t", '[', TelexShortcut::HornU, false).text, "tư");
        assert_eq!(apply("m", ']', TelexShortcut::HornO, false).text, "mơ");
        assert_eq!(apply("", 'W', TelexShortcut::LeadingW, false).text, "Ư");
        assert_eq!(apply("ư", 'w', TelexShortcut::LeadingW, false).text, "w");
    }

    #[test]
    fn spell_check_reuses_literal_fallback() {
        let result = apply("text", '[', TelexShortcut::HornU, true);
        assert_eq!(result.kind, TransformKind::Pending);
        assert_eq!(result.text, "text[");
    }
}
