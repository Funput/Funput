//! Two edits at once: any single edit, then a substitution somewhere it did not
//! touch.

use super::substitute::Substitute;
use super::{Edit, Flow, Generator, Sink, Variant};
use crate::correction::evidence::Evidence;

/// Every variant of the first edit paired with every substitution the evidence
/// allows at the positions that edit left alone. The costs add — a surcharge on top
/// was tried and only hid rivals, which doubled the wrong corrections — and a moved
/// key keeps the touch evidence of the key it came from, so a substitution after a
/// transposition is priced against the finger that pressed it.
///
/// Enumerated first-edit-major: for `Pair(Substitute)` that is exactly the nested
/// loop of the two-key search, each pair of positions visited once.
pub(crate) struct Pair<F>(pub(crate) F);

impl<F: Edit> Generator for Pair<F> {
    fn generate(&self, word: &Variant, evidence: &Evidence<'_>, sink: &mut Sink<'_>) -> Flow {
        self.0.each(word, evidence, &mut |first, scope, one| {
            Substitute.each_in(first, evidence, scope, &mut |second, _, two| {
                sink.offer(second.keys(), one + two, 2)
            })
        })
    }
}
