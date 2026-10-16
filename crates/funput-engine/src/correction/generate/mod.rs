//! Candidate generation: the edits that could turn the typed keys into a syllable.
//!
//! A *generator* enumerates variants of the word under one kind of edit and hands
//! each to the [`Sink`], which replays it through the composition pipeline and keeps
//! it when what comes out is a complete syllable. That reuses the Telex/VNI rules
//! instead of restating them — there is no second grammar to keep in step.
//!
//! Generators are grouped in cost [`Tier`]s, cheapest first, and the search is a
//! bounded replay: the sink counts, and a generator told the budget is spent stops
//! where it stands. A caller with its own tiers — a glide decoder, say — goes through
//! [`find_with`]; the keystroke path uses [`find`] with [`TIERS`]. Generators are
//! unit structs in a `const` table, never state: `Session` derives `Eq`.

mod replay;
mod sink;
mod substitute;

pub(crate) use sink::{MAX_REPLAYS_PER_WORD, Sink};

use substitute::{Pair, Substitute};

use crate::EngineConfig;
use crate::correction::evidence::Evidence;
use crate::correction::score;
use crate::correction::state::CorrectionState;
use crate::correction::touch::MAX_WORD_KEYS;

/// Shortest word worth correcting: one letter carries no evidence either way.
const MIN_WORD_KEYS: usize = 2;

/// One way to edit a word.
pub(crate) trait Generator: Sync {
    /// Offer every variant of `word` this edit reaches to `sink`, stopping as soon
    /// as it answers [`Flow::Exhausted`].
    fn generate(&self, word: &Variant, evidence: &Evidence<'_>, sink: &mut Sink<'_>) -> Flow;
}

/// Whether a generator may go on offering variants.
#[must_use]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Flow {
    Continue,
    /// The replay budget is spent: the last offer was refused, and the search ends.
    Exhausted,
}

/// The raw keys of one hypothesis. One slot more than the longest word, so an
/// insertion fits.
#[derive(Debug, Clone, Copy)]
pub(crate) struct Variant {
    pub(crate) keys: [char; MAX_WORD_KEYS + 1],
    /// For each position, the typed key whose touch evidence it inherits — `None`
    /// for a key no finger pressed. A substitution keeps it, a deletion shifts the
    /// tail, an insertion has nothing to inherit.
    pub(crate) origin: [Option<u8>; MAX_WORD_KEYS + 1],
    pub(crate) len: usize,
}

impl Variant {
    /// The word exactly as typed: every position its own evidence.
    pub(crate) fn typed(keys: &str) -> Self {
        let mut variant = Self {
            keys: ['\0'; MAX_WORD_KEYS + 1],
            origin: [None; MAX_WORD_KEYS + 1],
            len: 0,
        };
        for (i, key) in keys.chars().take(MAX_WORD_KEYS).enumerate() {
            variant.keys[i] = key;
            variant.origin[i] = Some(i as u8);
            variant.len = i + 1;
        }
        variant
    }

    pub(crate) fn keys(&self) -> &[char] {
        &self.keys[..self.len]
    }
}

/// Generators that cost about the same, run together before the next tier.
pub(crate) struct Tier {
    pub(crate) generators: &'static [&'static dyn Generator],
    /// The least any variant of this tier can cost, in nats — what lets a search
    /// stop early once nothing further down could still matter.
    pub(crate) min_cost: f32,
}

/// The edits the keystroke path searches: one neighbouring-key substitution, then two.
pub(crate) const TIERS: &[Tier] = &[Tier {
    generators: &[&Substitute, &Pair(Substitute, Substitute)],
    min_cost: 0.0,
}];

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
        ..
    } = state;
    replay::prepare(scratch, config);
    let evidence = Evidence::new(touch);
    // Kept beside the candidates it is measured against, so the ranking can read it
    // after the word — and its touch log — are gone.
    *typed_competes = evidence.has_full_touch();
    let word = Variant::typed(keys);
    *len = 0;
    let mut sink = Sink::new(scratch, candidates, len, shown, MAX_REPLAYS_PER_WORD);
    'search: for tier in tiers {
        for generator in tier.generators {
            if generator.generate(&word, &evidence, &mut sink) == Flow::Exhausted {
                break 'search;
            }
        }
    }
    score::rank(&mut candidates[..*len]);
    *len
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_typed_variant_gives_every_position_its_own_evidence() {
        let word = Variant::typed("nhad");
        assert_eq!(word.len, 4);
        assert_eq!(word.keys(), ['n', 'h', 'a', 'd']);
        assert_eq!(&word.origin[..4], [Some(0), Some(1), Some(2), Some(3)]);
        assert_eq!(word.origin[4], None);
    }

    #[test]
    fn a_variant_is_cut_at_the_key_cap() {
        let word = Variant::typed("abcdefghijklmnop");
        assert_eq!(word.len, MAX_WORD_KEYS);
        assert_eq!(word.keys().len(), MAX_WORD_KEYS);
    }

    #[test]
    fn the_tiers_run_cheapest_first() {
        assert!(
            TIERS
                .windows(2)
                .all(|pair| pair[0].min_cost <= pair[1].min_cost)
        );
        assert_eq!(TIERS[0].generators.len(), 2);
    }
}
