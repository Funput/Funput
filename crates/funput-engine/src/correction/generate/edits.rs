//! The structural edits of fast typing: a key that bounced, a modifier that strayed,
//! two fingers out of order, and a key that was simply not meant.

use funput_core::{KeyRole, key_role};

use super::{Each, Edit, Flow, MIN_WORD_KEYS, Scope, Variant};
use crate::correction::evidence::Evidence;
use crate::correction::score::cost;

/// A key pressed twice in a row: `chowii`. The second press goes, so the first —
/// and its touch — stays.
pub(crate) struct DupDelete;
/// A tone, shape, stroke or remove key where none belongs: `chowis`, `cho7ii`.
pub(crate) struct ModifierDelete;
/// Two neighbouring keys the other way round: `chwoi`.
pub(crate) struct Transpose;
/// Any other key that was not meant. Dearer than the two deletions above, which
/// already cover the keys they apply to.
pub(crate) struct Delete;

/// Whether the key at `at` repeats the one before it, case aside.
fn is_dup(word: &Variant, at: usize) -> bool {
    at > 0 && word.keys[at].eq_ignore_ascii_case(&word.keys[at - 1])
}

/// Whether the key at `at` is a modifier the method would act on.
fn is_modifier(word: &Variant, at: usize, evidence: &Evidence<'_>) -> bool {
    matches!(
        key_role(evidence.method(), word.keys[at]),
        KeyRole::Tone | KeyRole::Shape | KeyRole::Stroke | KeyRole::Remove
    )
}

/// Delete each position `wanted` picks, at `price`. A deletion never leaves fewer
/// than [`MIN_WORD_KEYS`]: a one-letter remainder is not a repaired syllable.
fn delete_where(
    word: &Variant,
    price: f32,
    wanted: impl Fn(usize) -> bool,
    each: &mut Each<'_>,
) -> Flow {
    if word.len <= MIN_WORD_KEYS {
        return Flow::Continue;
    }
    for at in (0..word.len).filter(|&at| wanted(at)) {
        if each(&word.deleted(at), Scope::ALL, price) == Flow::Exhausted {
            return Flow::Exhausted;
        }
    }
    Flow::Continue
}

impl Edit for DupDelete {
    fn each(&self, word: &Variant, _: &Evidence<'_>, each: &mut Each<'_>) -> Flow {
        delete_where(word, cost::DUP_DELETE, |at| is_dup(word, at), each)
    }
}

impl Edit for ModifierDelete {
    fn each(&self, word: &Variant, evidence: &Evidence<'_>, each: &mut Each<'_>) -> Flow {
        let wanted = |at| !is_dup(word, at) && is_modifier(word, at, evidence);
        delete_where(word, cost::MODIFIER_DELETE, wanted, each)
    }
}

impl Edit for Delete {
    fn each(&self, word: &Variant, evidence: &Evidence<'_>, each: &mut Each<'_>) -> Flow {
        let wanted = |at| !is_dup(word, at) && !is_modifier(word, at, evidence);
        delete_where(word, cost::DELETE, wanted, each)
    }
}

impl Edit for Transpose {
    fn each(&self, word: &Variant, _: &Evidence<'_>, each: &mut Each<'_>) -> Flow {
        for at in 0..word.len.saturating_sub(1) {
            // The same key twice, Shift or not, is a bounce — the dup edit's job.
            if is_dup(word, at + 1) {
                continue;
            }
            let scope = Scope {
                from: 0,
                skip: (1 << at) | (1 << (at + 1)),
            };
            if each(&word.transposed(at), scope, cost::TRANSPOSE) == Flow::Exhausted {
                return Flow::Exhausted;
            }
        }
        Flow::Continue
    }
}

#[cfg(test)]
mod tests {
    use funput_core::InputMethod;

    use super::*;

    /// Every variant an edit produces, as strings with their costs.
    fn variants(edit: &dyn Edit, keys: &str, method: InputMethod) -> Vec<(String, f32)> {
        let evidence = Evidence::none(method);
        let mut found = Vec::new();
        let flow = edit.each(&Variant::typed(keys), &evidence, &mut |variant, _, cost| {
            found.push((variant.keys().iter().collect(), cost));
            Flow::Continue
        });
        assert_eq!(flow, Flow::Continue);
        found
    }

    fn texts(edit: &dyn Edit, keys: &str, method: InputMethod) -> Vec<String> {
        variants(edit, keys, method)
            .into_iter()
            .map(|(text, _)| text)
            .collect()
    }

    #[test]
    fn a_doubled_key_loses_its_second_press() {
        assert_eq!(texts(&DupDelete, "chowii", InputMethod::Telex), ["chowi"]);
        assert_eq!(texts(&DupDelete, "Aab", InputMethod::Telex), ["Ab"]);
        assert_eq!(
            variants(&DupDelete, "chowii", InputMethod::Telex)[0].1,
            cost::DUP_DELETE
        );
    }

    #[test]
    fn a_stray_modifier_goes_but_a_doubled_one_is_the_dup_edit_s_job() {
        assert_eq!(
            texts(&ModifierDelete, "chowis", InputMethod::Telex),
            ["chois", "chowi"]
        );
        assert_eq!(
            texts(&ModifierDelete, "cho7ii", InputMethod::Vni),
            ["choii"]
        );
        assert_eq!(
            texts(&ModifierDelete, "chaass", InputMethod::Telex),
            ["chaas"]
        );
    }

    #[test]
    fn a_plain_deletion_skips_what_the_cheaper_deletions_cover() {
        assert_eq!(
            texts(&Delete, "chowis", InputMethod::Telex),
            ["howis", "cowis", "chwis", "chows"]
        );
    }

    #[test]
    fn no_deletion_leaves_a_single_key() {
        assert!(texts(&Delete, "ak", InputMethod::Telex).is_empty());
        assert!(texts(&DupDelete, "aa", InputMethod::Telex).is_empty());
    }

    #[test]
    fn a_transposition_swaps_each_differing_pair_once() {
        assert_eq!(
            texts(&Transpose, "chwoi", InputMethod::Telex),
            ["hcwoi", "cwhoi", "chowi", "chwio"]
        );
        assert_eq!(texts(&Transpose, "aab", InputMethod::Telex), ["aba"]);
        assert_eq!(texts(&Transpose, "Aab", InputMethod::Telex), ["Aba"]);
    }
}
