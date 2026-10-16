//! The substitution generators: one key swapped for a neighbour, and two.

use super::{Flow, Generator, Sink, Variant};
use crate::correction::evidence::Evidence;

/// Every single-key substitution the evidence allows, in typing order and nearest
/// neighbour first.
pub(crate) struct Substitute;

/// Two substitutions at once: every variant of the first paired with every
/// substitution at a later position. Enumerated first-position-major, which is the
/// order the one nested loop this replaced produced.
pub(crate) struct Pair(pub(crate) Substitute, pub(crate) Substitute);

impl Substitute {
    /// Hand `each` every substitution of `word` at positions `from..`, with the
    /// position edited and what the swap costs. Stops at the first `Exhausted`.
    fn each(
        &self,
        word: &Variant,
        evidence: &Evidence<'_>,
        from: usize,
        each: &mut dyn FnMut(&Variant, usize, f32) -> Flow,
    ) -> Flow {
        let mut edited = *word;
        for position in from..word.len {
            for (key, cost) in evidence.neighbours(word.origin[position]).iter() {
                edited.keys[position] = key;
                if each(&edited, position, cost) == Flow::Exhausted {
                    return Flow::Exhausted;
                }
            }
            edited.keys[position] = word.keys[position];
        }
        Flow::Continue
    }
}

impl Generator for Substitute {
    fn generate(&self, word: &Variant, evidence: &Evidence<'_>, sink: &mut Sink<'_>) -> Flow {
        self.each(word, evidence, 0, &mut |edited, _, cost| {
            sink.offer(edited.keys(), cost, 1)
        })
    }
}

impl Generator for Pair {
    fn generate(&self, word: &Variant, evidence: &Evidence<'_>, sink: &mut Sink<'_>) -> Flow {
        self.0.each(word, evidence, 0, &mut |first, position, one| {
            self.1
                .each(first, evidence, position + 1, &mut |second, _, two| {
                    sink.offer(second.keys(), one + two, 2)
                })
        })
    }
}
