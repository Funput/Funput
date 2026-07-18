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
    if shortcut == TelexShortcut::LeadingW && !buffer.is_empty() {
        let text = if buffer == "Ư" { "W" } else { "w" }.to_owned();
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
