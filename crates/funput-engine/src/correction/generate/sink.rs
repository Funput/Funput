//! Where generators send their variants: the validator and the ranked set, behind
//! one call, with the replay budget counted in the same place.

use super::Flow;
use super::replay::replay;
use crate::correction::score::{self, CorrectionCandidate};
use crate::model::Session;

/// The most variants one word may replay through the pipeline. Each replay costs
/// about one allocation per key and about a microsecond, so this is the latency
/// knob: the same envelope the two-key search had at its worst (435), rounded. A
/// word that reaches it ends its search with what the cheaper tiers found.
pub(crate) const MAX_REPLAYS_PER_WORD: u32 = 450;

/// What takes a variant off a generator's hands: replays it, checks the result is a
/// syllable other than the word on screen, and offers it to the ranked set.
pub(crate) struct Sink<'a> {
    scratch: &'a mut Session,
    candidates: &'a mut [CorrectionCandidate],
    len: &'a mut usize,
    /// The word as the app shows it — a variant that composes to the same thing is
    /// no correction.
    shown: &'a str,
    replays: u32,
    budget: u32,
}

impl<'a> Sink<'a> {
    pub(super) fn new(
        scratch: &'a mut Session,
        candidates: &'a mut [CorrectionCandidate],
        len: &'a mut usize,
        shown: &'a str,
        budget: u32,
    ) -> Self {
        Self {
            scratch,
            candidates,
            len,
            shown,
            replays: 0,
            budget,
        }
    }

    pub(crate) fn replays(&self) -> u32 {
        self.replays
    }

    /// The cheapest candidate so far, in nats — what the tier loop measures the
    /// next tier's floor against.
    pub(crate) fn best_cost(&self) -> Option<f32> {
        self.candidates[..*self.len]
            .iter()
            .map(CorrectionCandidate::cost)
            .min_by(f32::total_cmp)
    }

    /// Offer the raw keys of one variant, with the cost of the edits that made it
    /// and how many there were.
    ///
    /// A cost never goes below zero: a swap the touch evidence favours strongly can
    /// price itself under the flat `λ` and past it, but no candidate is a better
    /// explanation than the word as typed by more than "exactly as likely" — and
    /// hosts read `touch_score() = -cost` as always negative.
    ///
    /// [`Flow::Exhausted`] means the budget was already spent and *this* variant was
    /// not replayed: the generator stops, and the search ends with what it has.
    pub(crate) fn offer(&mut self, keys: &[char], cost: f32, edits: u8) -> Flow {
        if self.replays >= self.budget {
            return Flow::Exhausted;
        }
        self.replays += 1;
        if !replay(self.scratch, keys) || self.scratch.buffer == self.shown {
            return Flow::Continue;
        }
        score::consider(
            self.candidates,
            self.len,
            &self.scratch.buffer,
            score::to_milli(cost.max(0.0)),
            edits,
        );
        Flow::Continue
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::correction::generate::replay::prepare;
    use crate::correction::state::MAX_CANDIDATES;
    use crate::model::Session;

    fn scratch() -> Session {
        let mut scratch = Session::new();
        prepare(&mut scratch, &Session::new().config);
        scratch
    }

    #[test]
    fn the_same_word_offered_twice_keeps_its_lower_cost() {
        let mut scratch = scratch();
        let mut candidates = [(); MAX_CANDIDATES].map(|()| CorrectionCandidate::default());
        let mut len = 0;
        let mut sink = Sink::new(&mut scratch, &mut candidates, &mut len, "nhad", 10);
        let nha = ['n', 'h', 'a', 'f'];
        assert_eq!(sink.offer(&nha, 2.0, 2), Flow::Continue);
        assert_eq!(sink.offer(&nha, 1.0, 1), Flow::Continue);
        assert_eq!(sink.offer(&nha, 3.0, 1), Flow::Continue);
        assert_eq!(len, 1);
        assert_eq!(candidates[0].text(), "nhà");
        assert!((candidates[0].cost() - 1.0).abs() < 0.001);
        assert_eq!(candidates[0].edits(), 1);
    }

    #[test]
    fn a_strongly_evidenced_swap_never_costs_less_than_nothing() {
        let mut scratch = scratch();
        let mut candidates = [(); MAX_CANDIDATES].map(|()| CorrectionCandidate::default());
        let mut len = 0;
        let mut sink = Sink::new(&mut scratch, &mut candidates, &mut len, "nhad", 10);
        assert_eq!(sink.offer(&['n', 'h', 'a', 'f'], -0.4, 1), Flow::Continue);
        assert_eq!(candidates[0].cost(), 0.0);
        assert!(candidates[0].touch_score() <= 0.0);
    }

    #[test]
    fn a_variant_that_shows_the_same_word_or_no_syllable_is_dropped() {
        let mut scratch = scratch();
        let mut candidates = [(); MAX_CANDIDATES].map(|()| CorrectionCandidate::default());
        let mut len = 0;
        let mut sink = Sink::new(&mut scratch, &mut candidates, &mut len, "nhà", 10);
        assert_eq!(sink.offer(&['n', 'h', 'a', 'f'], 1.0, 1), Flow::Continue);
        assert_eq!(sink.offer(&['n', 'h', 'q'], 1.0, 1), Flow::Continue);
        assert_eq!(len, 0);
    }

    #[test]
    fn the_budget_refuses_the_offer_that_would_overrun_it() {
        let mut scratch = scratch();
        let mut candidates = [(); MAX_CANDIDATES].map(|()| CorrectionCandidate::default());
        let mut len = 0;
        let mut sink = Sink::new(&mut scratch, &mut candidates, &mut len, "x", 2);
        assert_eq!(sink.offer(&['n', 'h', 'a', 'f'], 1.0, 1), Flow::Continue);
        assert_eq!(sink.offer(&['n', 'h', 'a', 's'], 1.0, 1), Flow::Continue);
        assert_eq!(sink.offer(&['n', 'h', 'a', 'x'], 0.5, 1), Flow::Exhausted);
        assert_eq!(len, 2, "the refused variant was not replayed");
    }
}
