//! What a revert does to one word, found by typing it through the real engine.
//!
//! Asking the engine rather than matching letter pairs keeps the list honest about
//! every path a revert can take — `ss` after a tone, `seemed` reverting its own
//! circumflex, `daddy` reverting `đ` — and keeps it in step when the core changes.

use funput_core::{ExtraOnsets, InputMethod, SyllableRules};
use funput_engine::Engine;

/// Every setup a user can type English under where a revert could differ: both
/// letter-keyed methods, with and without the extra onsets that let `f`/`j`/`w`/`z`
/// start a syllable (`ferry` only loses an `r` once `f` is an onset).
pub(crate) const SETUPS: [(InputMethod, SyllableRules); 4] = [
    (InputMethod::Telex, SyllableRules::STANDARD),
    (InputMethod::Telex, WIDE),
    (InputMethod::TelexAdvanced, SyllableRules::STANDARD),
    (InputMethod::TelexAdvanced, WIDE),
];

const WIDE: SyllableRules = SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);

/// The keys left once a revert has swallowed one of `word`'s, or `None` when every
/// key survives. Only a revert ever shortens the raw keys, so their length alone
/// says whether one happened.
pub(crate) fn shortened(word: &str, method: InputMethod, rules: SyllableRules) -> Option<String> {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.syllable_rules = rules;
        config.restore_doubled_keys = false;
    });
    for key in word.chars() {
        engine.process_char(key);
    }
    let keys = engine.keys();
    (keys.chars().count() < word.chars().count()).then(|| keys.to_owned())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_doubled_tone_key_shortens_the_word() {
        let telex = |word| shortened(word, InputMethod::Telex, SyllableRules::STANDARD);
        assert_eq!(telex("password").as_deref(), Some("pasword"));
        assert_eq!(telex("pass").as_deref(), Some("pas"));
        assert_eq!(telex("seemed").as_deref(), Some("semed"));
    }

    #[test]
    fn a_word_without_a_revert_keeps_every_key() {
        for word in ["access", "text", "card", "jazz", "the"] {
            assert_eq!(
                shortened(word, InputMethod::Telex, SyllableRules::STANDARD),
                None,
                "{word}"
            );
        }
    }

    #[test]
    fn extra_onsets_open_another_path() {
        assert_eq!(
            shortened("ferry", InputMethod::Telex, SyllableRules::STANDARD),
            None
        );
        assert_eq!(
            shortened("ferry", InputMethod::Telex, WIDE).as_deref(),
            Some("fery")
        );
    }
}
