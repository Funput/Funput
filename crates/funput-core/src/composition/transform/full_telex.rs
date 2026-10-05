use crate::input_method::TelexShortcut;
use crate::orthography::reposition_existing_tone;
use crate::{ComposeOptions, TransformKind, TransformResult};

use super::gates;

pub(super) fn apply(
    buffer: &str,
    key: char,
    shortcut: TelexShortcut,
    options: ComposeOptions,
) -> TransformResult {
    if shortcut == TelexShortcut::RepeatedW {
        return TransformResult {
            kind: TransformKind::Reverted,
            text: buffer.to_owned(),
        };
    }
    // Pressing a shortcut on the vowel it produces puts the literal key back,
    // keeping the onset in front of it: `ư` + `w` → `w`, `thư` + `w` → `thw`,
    // `mơ` + `[` → `m[`, `tư` + `]` → `t]`. Only a toneless vowel qualifies, so a
    // tone already placed on it is never silently dropped.
    if let Some((prefix, vowel)) = split_trailing_vowel(buffer, shortcut) {
        let literal = match shortcut {
            TelexShortcut::LeadingW if vowel == 'Ư' => 'W',
            TelexShortcut::LeadingW => 'w',
            _ => key,
        };
        let mut text = String::with_capacity(prefix.len() + literal.len_utf8());
        text.push_str(prefix);
        text.push(literal);
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
    // A tone typed before the shortcut vowel sits where that vowel now outranks
    // it — the `gi` glide (`gĩ` + `w` → `giữ`) or the `u` of `uơ` (`thủ` + `[` →
    // `thuở`). Move it now, as every ordinary key does, so a word ending here is
    // already right.
    let text = reposition_existing_tone(&text, options.tone_style).unwrap_or(text);
    let result = TransformResult {
        kind: TransformKind::Applied,
        text,
    };
    gates::spell_check(buffer, key, options, result)
}

/// Split a buffer that ends in the vowel `shortcut` produces (`ư`/`Ư` for `w`
/// and `]`, `ơ`/`Ơ` for `[`) into the part before it and that vowel.
fn split_trailing_vowel(buffer: &str, shortcut: TelexShortcut) -> Option<(&str, char)> {
    let (lower, upper) = match shortcut {
        TelexShortcut::LeadingW | TelexShortcut::HornU => ('ư', 'Ư'),
        TelexShortcut::HornO => ('ơ', 'Ơ'),
        TelexShortcut::RepeatedW => return None,
    };
    let vowel = buffer
        .chars()
        .next_back()
        .filter(|&c| c == lower || c == upper)?;
    Some((&buffer[..buffer.len() - vowel.len_utf8()], vowel))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{InputMethod, ToneStyle};

    const PLAIN: ComposeOptions =
        ComposeOptions::new(InputMethod::TelexAdvanced).with_tone_style(ToneStyle::Traditional);
    const CHECKED: ComposeOptions = PLAIN.with_spell_check(true);

    #[test]
    fn shortcut_applies_and_leading_w_reverts() {
        assert_eq!(apply("t", ']', TelexShortcut::HornU, PLAIN).text, "tư");
        assert_eq!(apply("m", '[', TelexShortcut::HornO, PLAIN).text, "mơ");
        assert_eq!(apply("", 'W', TelexShortcut::LeadingW, PLAIN).text, "Ư");
        assert_eq!(apply("ư", 'w', TelexShortcut::LeadingW, PLAIN).text, "w");
    }

    #[test]
    fn second_bracket_restores_the_literal_key() {
        for (buffer, key, shortcut, output) in [
            ("mơ", '[', TelexShortcut::HornO, "m["),
            ("ơ", '[', TelexShortcut::HornO, "["),
            ("Ơ", '[', TelexShortcut::HornO, "["),
            ("tư", ']', TelexShortcut::HornU, "t]"),
            ("Ư", ']', TelexShortcut::HornU, "]"),
        ] {
            let result = apply(buffer, key, shortcut, PLAIN);
            assert_eq!(result.kind, TransformKind::Reverted, "{buffer}{key}");
            assert_eq!(result.text, output, "{buffer}{key}");
        }
        // The other bracket still composes, and a toned vowel is never reverted.
        assert_eq!(apply("ư", '[', TelexShortcut::HornO, PLAIN).text, "ươ");
        assert_eq!(
            apply("mớ", '[', TelexShortcut::HornO, PLAIN).kind,
            TransformKind::Applied
        );
    }

    #[test]
    fn shortcut_vowel_takes_the_tone_parked_on_the_glide() {
        assert_eq!(apply("gĩ", 'w', TelexShortcut::LeadingW, PLAIN).text, "giữ");
        assert_eq!(apply("gí", '[', TelexShortcut::HornO, PLAIN).text, "giớ");
    }

    #[test]
    fn spell_check_reuses_literal_fallback() {
        let result = apply("text", ']', TelexShortcut::HornU, CHECKED);
        assert_eq!(result.kind, TransformKind::Pending);
        assert_eq!(result.text, "text]");
    }
}
