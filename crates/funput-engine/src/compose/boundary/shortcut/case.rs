//! Smart case for gõ tắt: how a trigger was typed decides how its expansion comes
//! out (`vn` → as stored, `Vn` → first letter capitalized, `VN` → UPPERCASE), and
//! how it is looked up in the table. Case is only ever raised, never lowered: what
//! the user stored is the floor, the trigger's spelling as much as its expansion.
//! Only consulted while `shortcut_smart_case` is on.
//!
//! How a word reads is [`funput_core::shortcut`]'s answer, shared with the settings
//! code: a typed word is read leniently ([`typed_case`]), a stored trigger strictly
//! ([`stored_case`]), so `BBh` reaches `bbh` while a stored `PhD` stays exact.

use std::borrow::Cow;

use funput_core::shortcut::typed_case;
pub(super) use funput_core::shortcut::{TriggerCase, stored_case};

/// `word` folded the way the table indexes it, and the case it was typed in. With
/// smart case off, or for a word no Shift slip explains (`iOS`), that is the exact
/// word and no case. A lowercase word already is its own fold: only re-cased words
/// pay for a copy.
pub(super) fn table_form(word: &str, smart: bool) -> (Cow<'_, str>, Option<TriggerCase>) {
    let case = if smart { typed_case(word) } else { None };
    let form = match case {
        Some(TriggerCase::Lower) | None => Cow::Borrowed(word),
        Some(_) => Cow::Owned(word.to_lowercase()),
    };
    (form, case)
}

/// The case to raise an expansion to when a trigger stored as `stored` was typed as
/// `typed`: only a typed case above the stored one asks for more. A trigger kept as
/// `TP` gives its expansion as stored whether typed `tp`, `Tp` or `TP`.
pub(super) fn raise(typed: TriggerCase, stored: TriggerCase) -> Option<TriggerCase> {
    (typed > stored).then_some(typed)
}

/// Raise the case of `expansion` to match `case`. Nothing is ever lowered: a stored
/// `TP. HCM` stays `TP. HCM` under `Tp`. Unicode-aware `to_uppercase` keeps accented
/// Vietnamese letters right (`đ` → `Đ`, `ệ` → `Ệ`).
pub(super) fn apply(expansion: &str, case: TriggerCase) -> String {
    match case {
        TriggerCase::Lower => expansion.to_string(),
        TriggerCase::Upper => expansion.to_uppercase(),
        TriggerCase::Title => capitalize_first_letter(expansion),
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
