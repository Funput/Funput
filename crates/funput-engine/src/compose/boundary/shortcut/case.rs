//! Smart case for gõ tắt: how a trigger was typed decides how its expansion comes
//! out, and how it is looked up in the table.

use std::borrow::Cow;

/// The letter-casing pattern of typed keys, used to raise the expansion's case to
/// match (`vn` → as stored, `Vn` → first letter capitalized, `VN` → UPPERCASE), the
/// way UniKey does. Case is only ever raised, never lowered: what the user stored in
/// the table is the floor. Only consulted while `shortcut_smart_case` is on.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(super) enum ShortcutCase {
    Lower,
    Upper,
    Title,
}

/// `word` as the table spells it, and the case it was typed in. With smart case off,
/// or for a mixed-case word (`iOS`), that is the exact word and no case to raise. A
/// lowercase word already is its own spelling: only re-cased words pay for a copy.
pub(super) fn table_form(word: &str, smart: bool) -> (Cow<'_, str>, Option<ShortcutCase>) {
    let case = if smart { classify_case(word) } else { None };
    let form = match case {
        Some(ShortcutCase::Lower) | None => Cow::Borrowed(word),
        Some(_) => Cow::Owned(word.to_lowercase()),
    };
    (form, case)
}

/// Classify the case pattern of `keys` from its cased characters (letters), ignoring
/// digits and punctuation. Returns `None` when the keys don't fit a clean pattern
/// (e.g. `vNa`), so the caller falls back to an exact, unmodified lookup — this keeps
/// deliberately mixed-case triggers (like `iOS`) working exactly as defined. That same
/// `None` path is what the smart-case switch reuses when it is off.
fn classify_case(keys: &str) -> Option<ShortcutCase> {
    let mut letters = keys.chars().filter(|c| c.is_alphabetic());
    let first = letters.next()?;
    let rest: Vec<char> = letters.collect();
    if first.is_lowercase() {
        rest.iter()
            .all(|c| c.is_lowercase())
            .then_some(ShortcutCase::Lower)
    } else if !rest.is_empty() && rest.iter().all(|c| c.is_uppercase()) {
        Some(ShortcutCase::Upper)
    } else if rest.iter().all(|c| c.is_lowercase()) {
        // A single uppercase letter (`rest` empty) also lands here: capitalizing one
        // word reads more naturally than shouting the whole expansion.
        Some(ShortcutCase::Title)
    } else {
        None
    }
}

/// Raise the case of `expansion` to match `case`. Nothing is ever lowered: a stored
/// `TP. HCM` stays `TP. HCM` under `Tp`. Unicode-aware `to_uppercase` keeps accented
/// Vietnamese letters right (`đ` → `Đ`, `ệ` → `Ệ`).
pub(super) fn apply(expansion: &str, case: ShortcutCase) -> String {
    match case {
        ShortcutCase::Lower => expansion.to_string(),
        ShortcutCase::Upper => expansion.to_uppercase(),
        ShortcutCase::Title => capitalize_first_letter(expansion),
    }
}

/// Uppercase the first letter of `expansion`, leaving every other char as stored.
/// Leading non-alphanumerics (brackets, quotes, whitespace) are skipped over; an
/// expansion whose first alphanumeric is a digit (`3 con mèo`) comes back unchanged.
fn capitalize_first_letter(expansion: &str) -> String {
    let Some((at, letter)) = expansion
        .char_indices()
        .find(|(_, c)| c.is_alphanumeric())
        .filter(|(_, c)| c.is_alphabetic())
    else {
        return expansion.to_string();
    };
    let mut result = String::with_capacity(expansion.len());
    result.push_str(&expansion[..at]);
    result.extend(letter.to_uppercase());
    result.push_str(&expansion[at + letter.len_utf8()..]);
    result
}
