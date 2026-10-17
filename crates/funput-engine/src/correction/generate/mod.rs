//! Candidate generation: the edits that could turn the typed keys into a syllable.
//!
//! A *generator* enumerates variants of the word under one kind of edit and hands
//! each to the [`Sink`], which replays it through the composition pipeline and keeps
//! it when what comes out is a complete syllable. That reuses the Telex/VNI rules
//! instead of restating them — there is no second grammar to keep in step.
//!
//! Generators run in cost [`Tier`]s, cheapest first, and the search stops after a
//! tier once nothing a later tier could find would land within the confidence margin
//! of the best so far — so the common slip is settled by the cheap edits and only the
//! odd word pays for insertions. The sink counts replays against a budget, and a
//! generator told it is spent stops where it stands. A caller with its own tiers — a
//! glide decoder, say — goes through [`find_with`]; the keystroke path uses [`find`]
//! with [`TIERS`]. Generators are unit structs in a `const` table, never state:
//! `Session` derives `Eq`.

mod combo;
mod edits;
mod insert;
mod replay;
mod sink;
mod substitute;
mod tiers;
mod variant;

pub(crate) use sink::{MAX_REPLAYS_PER_WORD, Sink};
pub(crate) use tiers::find;
pub(crate) use variant::Variant;

use crate::correction::evidence::Evidence;

/// Shortest word worth correcting: one letter carries no evidence either way. Also
/// the shortest a deletion may leave behind.
pub(crate) const MIN_WORD_KEYS: usize = 2;

/// One way to edit a word.
pub(crate) trait Generator: Sync {
    /// Offer every variant of `word` this edit reaches to `sink`, stopping as soon
    /// as it answers [`Flow::Exhausted`].
    fn generate(&self, word: &Variant, evidence: &Evidence<'_>, sink: &mut Sink<'_>) -> Flow;
}

/// A single edit, enumerated so that another edit can be stacked on each variant.
pub(crate) trait Edit: Sync {
    /// Hand `each` every variant one application of this edit produces, with the
    /// cost of that application and where a second edit may still go.
    fn each(&self, word: &Variant, evidence: &Evidence<'_>, each: &mut Each<'_>) -> Flow;
}

/// The callback an [`Edit`] feeds.
pub(crate) type Each<'c> = dyn FnMut(&Variant, Scope, f32) -> Flow + 'c;

/// Where a second substitution may go after a first edit: positions from `from`,
/// except those whose bit is set in `skip`.
#[derive(Debug, Clone, Copy)]
pub(crate) struct Scope {
    pub(crate) from: usize,
    pub(crate) skip: u16,
}

impl Scope {
    pub(crate) const ALL: Self = Self { from: 0, skip: 0 };

    pub(crate) fn allows(self, position: usize) -> bool {
        position >= self.from && self.skip & (1 << position) == 0
    }
}

/// Every single edit is a generator on its own: each variant goes straight to the
/// sink as a one-edit candidate.
impl<E: Edit> Generator for E {
    fn generate(&self, word: &Variant, evidence: &Evidence<'_>, sink: &mut Sink<'_>) -> Flow {
        self.each(word, evidence, &mut |edited, _, cost| {
            sink.offer(edited.keys(), cost, 1)
        })
    }
}

/// Whether a generator may go on offering variants.
#[must_use]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Flow {
    Continue,
    /// The replay budget is spent: the last offer was refused, and the search ends.
    Exhausted,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_scope_starts_at_from_and_skips_the_masked_positions() {
        let scope = Scope {
            from: 2,
            skip: 1 << 3,
        };
        assert!(!scope.allows(1));
        assert!(scope.allows(2));
        assert!(!scope.allows(3));
        assert!(scope.allows(4));
        assert!(Scope::ALL.allows(0));
    }
}
