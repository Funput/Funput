//! The letter case of a trigger, read two ways: as it sits in the table, and as it
//! was just typed.
//!
//! Both readings look at letters only. Digits and punctuation carry no case, so
//! `k1`, `K1`, `;ct` and `;CT` classify by their letters alone.

/// The letter-case pattern of a trigger: `vn`, `Vn`, `VN`.
///
/// Ranked `Lower < Title < Upper`. An expansion is only ever raised to the case
/// its trigger was typed in, never lowered below what the user stored.
#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Debug)]
pub enum TriggerCase {
    /// Every letter lowercase.
    Lower,
    /// The first letter uppercase, the rest lowercase.
    Title,
    /// Every letter uppercase, with at least two letters.
    Upper,
}

/// The case of a trigger as stored in the table.
///
/// Strict: only a clean pattern has a case. A mixed spelling (`iOS`, `PhD`, `vNa`)
/// returns `None`, which keeps it a trigger that matches exactly as written and is
/// never folded or re-cased. A lone capital (`K`) is `Title`: capitalizing one word
/// reads better than shouting a whole expansion. A trigger with no letters at all
/// also returns `None`.
pub fn stored_case(trigger: &str) -> Option<TriggerCase> {
    let letters = Letters::scan(trigger)?;
    if letters.first_lower {
        letters.rest_lower.then_some(TriggerCase::Lower)
    } else if letters.is_upper() {
        Some(TriggerCase::Upper)
    } else {
        letters.rest_lower.then_some(TriggerCase::Title)
    }
}

/// The case of a word as the user just typed it.
///
/// Lenient where [`stored_case`] is strict about a capital first letter: anything
/// short of all capitals reads as `Title`. Typing fast, Shift held a beat too long
/// turns `Bbh` into `BBh`, and a trigger is a made-up string nobody means to type
/// in mixed case. A lowercase first letter followed by capitals (`bBh`, `iOS`) is
/// no slip of Shift, and stays `None`.
pub fn typed_case(word: &str) -> Option<TriggerCase> {
    let letters = Letters::scan(word)?;
    if letters.first_lower {
        letters.rest_lower.then_some(TriggerCase::Lower)
    } else if letters.is_upper() {
        Some(TriggerCase::Upper)
    } else {
        Some(TriggerCase::Title)
    }
}

/// What a case reading needs to know about the letters of a word.
struct Letters {
    first_lower: bool,
    /// Every letter after the first is lowercase (true when there are none).
    rest_lower: bool,
    /// Every letter after the first is uppercase (true when there are none).
    rest_upper: bool,
    rest_empty: bool,
}

impl Letters {
    /// `None` when `word` has no letters.
    fn scan(word: &str) -> Option<Self> {
        let mut letters = word.chars().filter(|c| c.is_alphabetic());
        let first = letters.next()?;
        let mut scan = Self {
            first_lower: first.is_lowercase(),
            rest_lower: true,
            rest_upper: true,
            rest_empty: true,
        };
        for letter in letters {
            scan.rest_empty = false;
            scan.rest_lower &= letter.is_lowercase();
            scan.rest_upper &= letter.is_uppercase();
        }
        Some(scan)
    }

    /// A capital first letter followed by at least one letter, all capitals.
    fn is_upper(&self) -> bool {
        !self.first_lower && !self.rest_empty && self.rest_upper
    }
}

#[cfg(test)]
mod tests {
    use super::TriggerCase::{Lower, Title, Upper};
    use super::{stored_case, typed_case};

    #[test]
    fn clean_patterns_read_the_same_both_ways() {
        for (word, case) in [
            ("vn", Lower),
            ("Vn", Title),
            ("VN", Upper),
            ("K", Title),
            ("k1", Lower),
            ("K1", Title),
            ("đc", Lower),
            ("ĐC", Upper),
            (";ct", Lower),
            (";CT", Upper),
        ] {
            assert_eq!(stored_case(word), Some(case), "stored {word}");
            assert_eq!(typed_case(word), Some(case), "typed {word}");
        }
    }

    #[test]
    fn a_capital_first_letter_short_of_all_caps_is_typed_title_case() {
        for word in ["BBh", "BbH", "CTsg", "PhD"] {
            assert_eq!(typed_case(word), Some(Title), "{word}");
        }
    }

    #[test]
    fn a_stored_mixed_spelling_has_no_case() {
        for word in ["BBh", "BbH", "PhD", "iOS", "vNa"] {
            assert_eq!(stored_case(word), None, "{word}");
        }
    }

    #[test]
    fn a_lowercase_first_letter_followed_by_capitals_has_no_case() {
        for word in ["bBh", "iOS", "vNA"] {
            assert_eq!(typed_case(word), None, "{word}");
        }
    }

    #[test]
    fn a_word_without_letters_has_no_case() {
        for word in ["", "500", ";;"] {
            assert_eq!(stored_case(word), None, "{word}");
            assert_eq!(typed_case(word), None, "{word}");
        }
    }
}
