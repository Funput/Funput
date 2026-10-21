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

/// The engine's state after one key of a word.
pub(crate) struct Step {
    /// Whether a revert has swallowed a key at or before this one.
    pub(crate) reverted: bool,
    /// What the app shows for the word.
    pub(crate) buffer: String,
    /// The raw keys as the engine holds them, shortened by any revert.
    pub(crate) keys: String,
}

/// Type `word` key by key with doubled-key restore off, which is the state the
/// feature itself decides from. A revert is the only keystroke that leaves a live
/// word's keys no longer than they were, so that alone says when one happened.
pub(crate) fn typed(word: &str, method: InputMethod, rules: SyllableRules) -> Vec<Step> {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.syllable_rules = rules;
        config.restore_doubled_keys = false;
    });
    let mut reverted = false;
    word.chars()
        .map(|key| {
            let before = engine.keys().chars().count();
            engine.process_char(key);
            let after = engine.keys().chars().count();
            reverted |= after > 0 && after <= before;
            Step {
                reverted,
                buffer: engine.buffer().to_owned(),
                keys: engine.keys().to_owned(),
            }
        })
        .collect()
}

/// The keys left once a revert has swallowed one of `word`'s, or `None` when every
/// key survives.
pub(crate) fn shortened(word: &str, method: InputMethod, rules: SyllableRules) -> Option<String> {
    let last = typed(word, method, rules).pop()?;
    last.reverted.then_some(last.keys)
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
    fn each_step_says_what_is_shown_and_whether_a_revert_happened() {
        let steps = typed("pass", InputMethod::Telex, SyllableRules::STANDARD);
        let shown: Vec<_> = steps
            .iter()
            .map(|step| (step.reverted, step.buffer.as_str()))
            .collect();
        assert_eq!(
            shown,
            [(false, "p"), (false, "pa"), (false, "pá"), (true, "pas")]
        );
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
