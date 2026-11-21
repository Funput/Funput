//! Which triggers can ever fire, and when two triggers stand for the same word.
//!
//! The engine looks a trigger up word by word: punctuation and whitespace end a
//! word, so a trigger can only hold symbols where the engine joins them back on.
//! That is a prefix of ASCII symbols and digits typed in front of the word (`;ct`,
//! `2k`) and one symbol that is the boundary key ending it (`ct;`). Anything else
//! (`c t`, `x.y`) would sit in the table and never expand, so the settings screens
//! refuse it with [`check_trigger`] instead.

use std::fmt;

use super::stored_case;

/// The longest symbol prefix a trigger may open with, in bytes: as many as the
/// engine keeps in front of a word.
pub const MAX_PREFIX: usize = 8;

/// Why a trigger can never fire. [`fmt::Display`] gives the sentence a settings
/// screen shows under the field.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TriggerError {
    Empty,
    Whitespace,
    /// Only symbols and digits: there is no word to type.
    NoWord,
    /// A symbol inside the word (`x.y`), which ends the word before it is done.
    InnerSymbol,
    /// More than [`MAX_PREFIX`] symbols in front.
    LongPrefix,
    /// More than one symbol after the word (`ct;;`).
    LongSuffix,
    /// Ends with `@` or `/`, which end a word without expanding it.
    SilentSuffix,
}

impl fmt::Display for TriggerError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::Empty => "Chữ tắt đang trống.",
            Self::Whitespace => "Chữ tắt không được có dấu cách.",
            Self::NoWord => "Chữ tắt cần có chữ, không chỉ có ký hiệu hay số.",
            Self::InnerSymbol => "Ký hiệu chỉ được đứng ở đầu hoặc cuối chữ tắt.",
            Self::LongPrefix => "Phần ký hiệu ở đầu dài quá 8 ký tự.",
            Self::LongSuffix => "Ở cuối chữ tắt chỉ được có một ký hiệu.",
            Self::SilentSuffix => "Chữ tắt không được kết thúc bằng @ hoặc /.",
        })
    }
}

impl std::error::Error for TriggerError {}

/// Whether `trigger` can ever fire: symbols and digits in front, a word, and at
/// most one symbol after it.
///
/// `[` and `]` count as letters, since advanced Telex composes them into `ư` and
/// `ơ`. A non-ASCII symbol (`“`) is part of the word, as the engine reads it.
pub fn check_trigger(trigger: &str) -> Result<(), TriggerError> {
    if trigger.is_empty() {
        return Err(TriggerError::Empty);
    }
    if trigger.chars().any(char::is_whitespace) {
        return Err(TriggerError::Whitespace);
    }
    let prefix = trigger
        .bytes()
        .take_while(|&b| is_symbol(b) || b.is_ascii_digit())
        .count();
    let rest = &trigger[prefix..];
    let (word, suffix) = rest.split_at(rest.trim_end_matches(is_symbol_char).len());
    if word.is_empty() {
        Err(TriggerError::NoWord)
    } else if word.bytes().any(is_symbol) {
        Err(TriggerError::InnerSymbol)
    } else if prefix > MAX_PREFIX {
        Err(TriggerError::LongPrefix)
    } else if suffix.len() > 1 {
        Err(TriggerError::LongSuffix)
    } else if matches!(suffix, "@" | "/") {
        Err(TriggerError::SilentSuffix)
    } else {
        Ok(())
    }
}

/// How two stored triggers relate, for a table that must not hold the same one
/// twice.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TriggerOverlap {
    /// Spelled the same: only one of them can ever fire.
    Same,
    /// The same word in another clean case (`tp`, `TP`). Both fire, each for its
    /// own spelling, and the one nearer lowercase answers the cases in between.
    CaseVariant,
    Distinct,
}

/// How `a` and `b` relate as stored triggers. Mixed spellings (`iOS`) only ever
/// match exactly, so they are never case variants of anything.
pub fn compare_triggers(a: &str, b: &str) -> TriggerOverlap {
    if a == b {
        TriggerOverlap::Same
    } else if stored_case(a).is_some()
        && stored_case(b).is_some()
        && a.to_lowercase() == b.to_lowercase()
    {
        TriggerOverlap::CaseVariant
    } else {
        TriggerOverlap::Distinct
    }
}

/// An ASCII symbol that ends a word. `[` and `]` are left out: advanced Telex
/// types letters with them.
fn is_symbol(byte: u8) -> bool {
    byte.is_ascii_punctuation() && !matches!(byte, b'[' | b']')
}

fn is_symbol_char(c: char) -> bool {
    c.is_ascii() && is_symbol(c as u8)
}

#[cfg(test)]
mod tests {
    use super::TriggerError::*;
    use super::TriggerOverlap::*;
    use super::{check_trigger, compare_triggers};

    #[test]
    fn triggers_that_can_fire_pass() {
        for trigger in [
            "vn", "ct", "đc", "k2", "iOS", ";ct", ";;ct", "2k", "ct;", ";ct;", "[ct", "tr]", "“vn”",
        ] {
            assert_eq!(check_trigger(trigger), Ok(()), "{trigger:?}");
        }
    }

    #[test]
    fn triggers_that_never_fire_are_refused_with_a_reason() {
        for (trigger, error) in [
            ("", Empty),
            ("c t", Whitespace),
            (" vn", Whitespace),
            (";", NoWord),
            ("500", NoWord),
            ("x.y", InnerSymbol),
            ("a;b;", InnerSymbol),
            (";;;;;;;;;ct", LongPrefix),
            ("ct;;", LongSuffix),
            ("ct@", SilentSuffix),
            ("ct/", SilentSuffix),
        ] {
            assert_eq!(check_trigger(trigger), Err(error), "{trigger:?}");
        }
    }

    #[test]
    fn every_reason_reads_as_a_sentence() {
        let reasons = [
            Empty,
            Whitespace,
            NoWord,
            InnerSymbol,
            LongPrefix,
            LongSuffix,
            SilentSuffix,
        ];
        for error in reasons {
            assert!(error.to_string().ends_with('.'), "{error:?}");
        }
    }

    #[test]
    fn overlap_follows_how_the_engine_reaches_a_trigger() {
        assert_eq!(compare_triggers("ct", "ct"), Same);
        assert_eq!(compare_triggers("tp", "TP"), CaseVariant);
        assert_eq!(compare_triggers("Tp", "tp"), CaseVariant);
        assert_eq!(compare_triggers(";ct", ";CT"), CaseVariant);
        assert_eq!(compare_triggers("iOS", "ios"), Distinct);
        assert_eq!(compare_triggers("ct", "cta"), Distinct);
    }
}
