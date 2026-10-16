//! The substitution: one key swapped for a neighbour the evidence allows.

use super::{Each, Edit, Flow, Scope, Variant};
use crate::correction::evidence::Evidence;

/// Every single-key substitution the evidence allows, in typing order and nearest
/// neighbour first.
pub(crate) struct Substitute;

impl Substitute {
    /// [`Edit::each`] limited to the positions `scope` allows — how a pair stacks a
    /// substitution on an edit without touching what that edit touched, and how two
    /// substitutions avoid visiting the same pair of positions twice.
    pub(super) fn each_in(
        &self,
        word: &Variant,
        evidence: &Evidence<'_>,
        scope: Scope,
        each: &mut Each<'_>,
    ) -> Flow {
        let mut edited = *word;
        for position in (scope.from..word.len).filter(|&p| scope.allows(p)) {
            let neighbours = evidence.neighbours(word.origin[position], word.keys[position]);
            for (key, cost) in neighbours.iter() {
                edited.keys[position] = key;
                let after = Scope {
                    from: position + 1,
                    skip: scope.skip,
                };
                if each(&edited, after, cost) == Flow::Exhausted {
                    return Flow::Exhausted;
                }
            }
            edited.keys[position] = word.keys[position];
        }
        Flow::Continue
    }
}

impl Edit for Substitute {
    fn each(&self, word: &Variant, evidence: &Evidence<'_>, each: &mut Each<'_>) -> Flow {
        self.each_in(word, evidence, Scope::ALL, each)
    }
}
