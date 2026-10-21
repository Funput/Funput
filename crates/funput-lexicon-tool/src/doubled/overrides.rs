//! Hand corrections to the generated list, one per line: `+word` lists a word the
//! scan leaves out, `-word` drops one it keeps. Keep the reason in a comment above.
//!
//! An override that changes nothing is an error rather than a no-op, the same rule
//! the blocklist's re-admits follow: it is a typo, or the scan moved under it and
//! the line is now stale.

use std::collections::BTreeSet;

use crate::forms;

#[derive(Debug, Default, PartialEq, Eq)]
pub(crate) struct Overrides {
    add: Vec<String>,
    remove: Vec<String>,
}

impl Overrides {
    pub(crate) fn parse(text: &str) -> Result<Self, String> {
        let mut overrides = Self::default();
        for line in forms::entries(text) {
            let (list, word) = if let Some(word) = line.strip_prefix('+') {
                (&mut overrides.add, word)
            } else if let Some(word) = line.strip_prefix('-') {
                (&mut overrides.remove, word)
            } else {
                return Err(format!("override {line:?} must start with + or -"));
            };
            if !forms::admissible(word) {
                return Err(format!("override {line:?} is not a plain ASCII word"));
            }
            list.push(word.to_ascii_lowercase());
        }
        Ok(overrides)
    }

    pub(crate) fn apply(&self, words: &mut BTreeSet<String>) -> Result<(), String> {
        for word in &self.add {
            if !words.insert(word.clone()) {
                return Err(format!("+{word} is already listed"));
            }
        }
        for word in &self.remove {
            if !words.remove(word) {
                return Err(format!("-{word} is not listed"));
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn listed(words: &[&str]) -> BTreeSet<String> {
        words.iter().map(|word| (*word).to_owned()).collect()
    }

    #[test]
    fn adds_and_removes_skipping_comments() {
        let overrides = Overrides::parse("# why\n+Hiss\n\n-pass\n").unwrap();
        let mut words = listed(&["boss", "pass"]);
        overrides.apply(&mut words).unwrap();
        assert_eq!(words, listed(&["boss", "hiss"]));
    }

    #[test]
    fn refuses_a_line_without_a_sign_or_with_a_phrase() {
        assert!(Overrides::parse("pass\n").is_err());
        assert!(Overrides::parse("+pass word\n").is_err());
        assert!(Overrides::parse("+\n").is_err());
    }

    #[test]
    fn refuses_an_override_that_changes_nothing() {
        let mut words = listed(&["pass"]);
        assert!(
            Overrides::parse("+pass")
                .unwrap()
                .apply(&mut words)
                .is_err()
        );
        assert!(
            Overrides::parse("-boss")
                .unwrap()
                .apply(&mut words)
                .is_err()
        );
    }
}
