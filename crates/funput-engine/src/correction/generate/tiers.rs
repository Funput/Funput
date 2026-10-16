//! The cost tiers of the keystroke search, and the search loop that runs them.

use super::combo::Pair;
use super::edits::{Delete, DupDelete, ModifierDelete, Transpose};
use super::insert::Insert;
use super::substitute::Substitute;
use super::{Flow, Generator, MAX_REPLAYS_PER_WORD, MIN_WORD_KEYS, Sink, Variant, replay};
use crate::EngineConfig;
use crate::correction::evidence::Evidence;
use crate::correction::score::{self, MARGIN};
use crate::correction::state::CorrectionState;
use crate::correction::touch::MAX_WORD_KEYS;

/// Generators that cost about the same, run together before the next tier.
pub(crate) struct Tier {
    pub(crate) generators: &'static [&'static dyn Generator],
    /// The least any variant of this tier can cost, in nats — what lets a search
    /// stop early once nothing further down could still matter.
    pub(crate) min_cost: f32,
}

/// The edits the keystroke path searches, in three tiers by the least a variant of
/// each can cost, and within a tier the fewest replays first: the four single edits
/// and the two cheap pairs come before the insertions, which try every letter.
///
/// The floors are true lower bounds given that the sink clamps a cost at zero: a
/// substitution with strong touch evidence can be free, so a tier that holds one —
/// alone or in a pair — floors at the other edit's price or at nothing. Under the
/// stop rule that makes the first two tiers always run, and the third only when the
/// best reading so far costs more than `MARGIN`: a bounce (`DUP_DELETE` 1.4) or a
/// slip with touch evidence settles without a single insertion; a dearer first
/// reading runs on to the budget.
pub(crate) const TIERS: &[Tier] = &[
    Tier {
        generators: &[&Substitute, &DupDelete, &ModifierDelete, &Transpose],
        min_cost: 0.0,
    },
    Tier {
        generators: &[&Pair(Substitute), &Pair(DupDelete)],
        min_cost: 0.0,
    },
    Tier {
        generators: &[
            &Delete,
            &Insert,
            &Pair(ModifierDelete),
            &Pair(Transpose),
            &Pair(Delete),
        ],
        min_cost: 3.0,
    },
];

/// Fill the ranked candidate set for the word `keys` produced, and return how many
/// candidates were found.
pub(crate) fn find(
    state: &mut CorrectionState,
    config: &EngineConfig,
    keys: &str,
    shown: &str,
) -> usize {
    find_with(TIERS, state, config, keys, shown)
}

/// [`find`] with the caller's own tiers.
///
/// After each tier the search asks whether the next one could still change the
/// verdict: it cannot once the best cost so far plus the margin is no more than
/// the least that tier can produce, so the search stops there. (A later tier may
/// still hold a candidate the host's prior lifts within the margin; the rule bounds
/// cost, not score.)
pub(crate) fn find_with(
    tiers: &[Tier],
    state: &mut CorrectionState,
    config: &EngineConfig,
    keys: &str,
    shown: &str,
) -> usize {
    debug_assert!(
        tiers
            .windows(2)
            .all(|pair| pair[0].min_cost <= pair[1].min_cost)
    );
    let count = keys.chars().count();
    if !(MIN_WORD_KEYS..=MAX_WORD_KEYS).contains(&count) {
        return 0;
    }
    let CorrectionState {
        touch,
        scratch,
        candidates,
        len,
        typed_competes,
        replays_last,
        ..
    } = state;
    replay::prepare(scratch, config);
    let evidence = Evidence::new(touch, keys, config.method);
    // Kept beside the candidates it is measured against, so the ranking can read it
    // after the word — and its touch log — are gone.
    *typed_competes = evidence.has_full_touch();
    let word = Variant::typed(keys);
    *len = 0;
    let mut sink = Sink::new(scratch, candidates, len, shown, MAX_REPLAYS_PER_WORD);
    'search: for (i, tier) in tiers.iter().enumerate() {
        for generator in tier.generators {
            if generator.generate(&word, &evidence, &mut sink) == Flow::Exhausted {
                break 'search;
            }
        }
        if let (Some(best), Some(next)) = (sink.best_cost(), tiers.get(i + 1))
            && best + MARGIN <= next.min_cost
        {
            break;
        }
    }
    *replays_last = sink.replays();
    score::rank(&mut candidates[..*len]);
    *len
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_tiers_run_cheapest_first() {
        assert!(
            TIERS
                .windows(2)
                .all(|pair| pair[0].min_cost <= pair[1].min_cost)
        );
        assert_eq!(TIERS.len(), 3);
    }
}
