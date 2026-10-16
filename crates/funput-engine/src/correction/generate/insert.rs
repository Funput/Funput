//! The insertion: a key that never registered.

use funput_core::{InputMethod, KeyRole, key_role};

use super::{Each, Edit, Flow, Scope, Variant};
use crate::correction::evidence::Evidence;
use crate::correction::score::cost;
use crate::correction::touch::MAX_WORD_KEYS;

/// Every letter at every position, plus the digit modifiers under VNI. The dearest
/// edit and the widest: a twelve-key word has thirteen gaps and 26 letters to try in
/// each, so it runs last and only when the cheaper edits left the verdict open.
pub(crate) struct Insert;

/// The keys worth inserting: the letters, and the digits the method reads.
fn alphabet(method: InputMethod) -> impl Iterator<Item = char> {
    ('a'..='z').chain(('0'..='9').filter(move |&digit| key_role(method, digit) != KeyRole::Other))
}

impl Edit for Insert {
    fn each(&self, word: &Variant, evidence: &Evidence<'_>, each: &mut Each<'_>) -> Flow {
        if word.len >= MAX_WORD_KEYS {
            return Flow::Continue;
        }
        for at in 0..=word.len {
            let scope = Scope {
                from: 0,
                skip: 1 << at,
            };
            for key in alphabet(evidence.method()) {
                let inserted = word.inserted(at, key);
                if each(&inserted, scope, cost::INSERT) == Flow::Exhausted {
                    return Flow::Exhausted;
                }
            }
        }
        Flow::Continue
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn texts(keys: &str, method: InputMethod) -> Vec<String> {
        let mut found = Vec::new();
        let flow = Insert.each(
            &Variant::typed(keys),
            &Evidence::none(method),
            &mut |v, _, cost| {
                assert_eq!(cost, cost::INSERT);
                found.push(v.keys().iter().collect::<String>());
                Flow::Continue
            },
        );
        assert_eq!(flow, Flow::Continue);
        found
    }

    #[test]
    fn every_letter_is_tried_in_every_gap() {
        let found = texts("ab", InputMethod::Telex);
        assert_eq!(found.len(), 3 * 26);
        assert!(found.contains(&"acb".to_owned()));
        assert!(found.contains(&"zab".to_owned()));
        assert!(found.contains(&"abz".to_owned()));
        assert!(
            !found.contains(&"a1b".to_owned()),
            "digits are not Telex keys"
        );
    }

    #[test]
    fn vni_also_tries_its_digit_modifiers() {
        let found = texts("ab", InputMethod::Vni);
        assert_eq!(found.len(), 3 * (26 + 10));
        assert!(found.contains(&"a1b".to_owned()));
    }

    #[test]
    fn an_inserted_key_follows_the_case_of_the_position_it_takes() {
        let found = texts("Ch", InputMethod::Telex);
        assert!(found.contains(&"Och".to_owned()));
        assert!(found.contains(&"Coh".to_owned()));
        assert!(found.contains(&"Cho".to_owned()));
    }

    #[test]
    fn a_word_at_the_cap_gets_no_insertion() {
        assert!(texts("abcdefghijkl", InputMethod::Telex).is_empty());
    }
}
