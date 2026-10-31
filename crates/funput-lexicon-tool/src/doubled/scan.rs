//! What a revert does to one word, found by typing it through the real engine.
//!
//! Asking the engine rather than matching letter pairs keeps the list honest about
//! every path a revert can take — `ss` after a tone, `seemed` reverting its own
//! circumflex, `daddy` reverting `đ` — and keeps it in step when the core changes.

use crate::wordlist::setup::Setup;

use super::habit;

/// The engine's state after one key of a word.
pub(crate) struct Step {
    /// Whether a revert has swallowed a key at or before this one.
    pub(crate) reverted: bool,
    /// What the app shows for the word.
    pub(crate) buffer: String,
    /// The keys as the engine holds them: raw until a revert, then rewritten to the
    /// screen at that point (`nêcs` for `necess`), with later keys appended.
    pub(crate) keys: String,
    /// The keys typed so far with every key that reverted its own twin dropped
    /// (`passw` → `pasw`): what a user doubling keys for plain letters means.
    pub(crate) meant: String,
}

/// Type `word` key by key with doubled-key restore off, which is the state the
/// feature itself decides from. A revert is the only keystroke that rewrites a live
/// word's keys instead of appending to them, so that alone says when one happened.
/// Their length does not: `tômr` + `o` reverts to `tomro`, one key longer.
pub(crate) fn typed(word: &str, setup: Setup) -> Vec<Step> {
    let mut engine = setup.engine(|config| config.restore_doubled_keys = false);
    let (mut typed, mut reverting) = (String::new(), Vec::new());
    word.chars()
        .map(|key| {
            let appended = format!("{}{key}", engine.keys());
            engine.process_char(key);
            if !engine.keys().is_empty() && engine.keys() != appended {
                reverting.push(typed.len());
            }
            typed.push(key);
            Step {
                reverted: !reverting.is_empty(),
                buffer: engine.buffer().to_owned(),
                keys: engine.keys().to_owned(),
                meant: habit::meant(&typed, reverting.iter().copied()),
            }
        })
        .collect()
}

/// The keys left once a revert has swallowed one of `word`'s, or `None` when every
/// key survives.
pub(crate) fn shortened(word: &str, setup: Setup) -> Option<String> {
    let last = typed(word, setup).pop()?;
    last.reverted.then_some(last.keys)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::wordlist::setup::SETUPS;

    #[test]
    fn a_doubled_tone_key_shortens_the_word() {
        let telex = |word| shortened(word, SETUPS[0]);
        assert_eq!(telex("password").as_deref(), Some("pasword"));
        assert_eq!(telex("pass").as_deref(), Some("pas"));
        assert_eq!(telex("seemed").as_deref(), Some("semed"));
    }

    #[test]
    fn a_word_without_a_revert_keeps_every_key() {
        for word in ["access", "text", "card", "jazz", "the"] {
            assert_eq!(shortened(word, SETUPS[0]), None, "{word}");
        }
    }

    #[test]
    fn each_step_says_what_is_shown_and_whether_a_revert_happened() {
        let steps = typed("pass", SETUPS[0]);
        let shown: Vec<_> = steps
            .iter()
            .map(|step| (step.reverted, step.buffer.as_str()))
            .collect();
        assert_eq!(
            shown,
            [(false, "p"), (false, "pa"), (false, "pá"), (true, "pas")]
        );
        assert_eq!(steps[3].meant, "pas");
    }

    #[test]
    fn extra_onsets_open_another_path() {
        assert_eq!(shortened("ferry", SETUPS[0]), None);
        assert_eq!(shortened("ferry", SETUPS[1]).as_deref(), Some("fery"));
    }

    #[test]
    fn eager_restore_off_opens_another_path() {
        // On, `cl` dead-ends and is shown raw before the second `s` arrives.
        assert_eq!(shortened("class", SETUPS[0]), None);
        assert_eq!(shortened("class", SETUPS[4]).as_deref(), Some("clas"));
    }
}
