//! The candidate type and the arithmetic that ranks candidates.
//!
//! The touch model is Gaussian: a finger landing `d` key-pitches from a key's centre
//! makes that key `exp(-d² / 2σ²)` likely, so in log space every key contributes
//! `-d² / 2σ²` and a word is the sum of its keys. Swapping one key for a neighbour
//! trades that key's term for the neighbour's and pays a flat `λ`, which stops a
//! two-key guess from beating a one-key guess on rounding noise alone.
//!
//! A candidate carries the *cost* of the edits that reached it, in nats: the flat
//! `λ` per edit plus, where a touch was reported, what the swap gives up in touch
//! evidence. The word as typed is the zero point, so its own base term never has to
//! be computed — every comparison the ranking makes is a difference against it.
//!
//! Costs are kept as milli-nats (`i32`) rather than `f32`: a candidate is reachable
//! from `Session`, which derives `Eq`.

mod choose;
mod set;

pub use choose::{CorrectionRanking, CorrectionVerdict};
pub(crate) use choose::{INVALID_COST, choose};
pub(crate) use set::{consider, rank};

/// Touch spread in key pitches — how far a finger typically lands from centre.
///
/// Deliberately wider than the 0.174 measured on a device (see [`INVALID_COST`]):
/// σ is the spread the *model* assumes, not the one observed. A narrow σ makes every
/// score fall off a cliff a fraction of a pitch from the key, so a touch anywhere
/// near an edge would hand the win to whichever key it is a hair closer to. At 0.45
/// a touch between two keys still gives both a fair hearing, and the prior and the
/// margin decide — which is what `nhad` typed dead-centre staying ambiguous relies
/// on. Recalibrate it only together with [`INVALID_COST`] and [`MARGIN`], with
/// `funput dev typos`.
pub(crate) const SIGMA: f32 = 0.45;
/// Flat cost of substituting one key, in nats.
pub(crate) const LAMBDA: f32 = 1.2;
/// How far ahead the winner must be before a correction is applied without asking.
///
/// Measured with `funput dev typos` over Viet74K at a finger spread of 0.25 pitches,
/// with the candidate set filtered to words the platform recognizes: 1.0 leaves 5.6%
/// of corrections wrong, 1.5 leaves 3.9%, and it costs 4 points of repair rate to buy
/// that. Past 1.5 the trade turns bad — 2.5 halves the repairs for another 1.6 points
/// — because wrong corrections do not come from two candidates being close together,
/// they come from the model being confidently wrong.
pub(crate) const MARGIN: f32 = 1.5;
/// Score a word the user has never typed still carries, so a correct-but-unseen word
/// is not shut out by a familiar one.
pub(crate) const PRIOR_FLOOR: f32 = 0.5;

/// One way the word could have been meant, with what it cost to get there.
///
/// The cost is the edit evidence *only*. How likely the word is — the user's own
/// use counts, an English lexicon — lives outside this crate, so the platform adds
/// its prior before choosing (see [`crate::Engine::choose_correction`]).
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct CorrectionCandidate {
    text: String,
    /// Positive milli-nats; lower is better.
    cost: i32,
    edits: u8,
}

impl CorrectionCandidate {
    /// The corrected word, ready to replace what is on screen.
    pub fn text(&self) -> &str {
        &self.text
    }

    /// Minus the cost of the edits that reach this word, in nats: always negative,
    /// closer to zero is better. Touch evidence is folded in where the host
    /// reported it. The sign is the public contract — hosts add a prior to it.
    pub fn touch_score(&self) -> f32 {
        -self.cost()
    }

    /// What reaching this word cost, in nats. Lower is better.
    pub(crate) fn cost(&self) -> f32 {
        from_milli(self.cost)
    }

    /// How many keys had to be substituted to reach this word.
    pub fn edits(&self) -> usize {
        usize::from(self.edits)
    }

    /// Refill in place so the string capacity survives into the next word.
    fn fill(&mut self, text: &str, cost: i32, edits: u8) {
        self.text.clear();
        self.text.push_str(text);
        self.cost = cost;
        self.edits = edits;
    }
}

/// log P(touch | key) for one key, in nats: zero for a touch dead on the centre.
fn key_term(distance: f32) -> f32 {
    -(distance * distance) / (2.0 * SIGMA * SIGMA)
}

/// What swapping the key that was typed for an alternate costs, in nats: the flat
/// `λ`, plus the touch evidence given up by moving from the key the finger was
/// `typed_distance` from to one it was `alternate_distance` from. Below `λ` when the
/// alternate was the nearer key.
pub(crate) fn substitution_cost(typed_distance: f32, alternate_distance: f32) -> f32 {
    LAMBDA + key_term(typed_distance) - key_term(alternate_distance)
}

/// How much a word's use count is worth, in nats. `ln(1 + uses)` grows slowly enough
/// that a familiar word cannot outvote two edits' worth of touch evidence.
pub(crate) fn word_prior(uses: u32) -> f32 {
    (1.0 + uses as f32).ln() + PRIOR_FLOOR
}

pub(crate) fn to_milli(nats: f32) -> i32 {
    (nats * 1000.0) as i32
}

pub(crate) fn from_milli(milli: i32) -> f32 {
    milli as f32 / 1000.0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_nearer_alternate_costs_less_than_the_edit_alone() {
        // The finger landed most of a key away from what it hit, and a hair from
        // its neighbour: the evidence gained outweighs the λ the swap costs.
        assert!(substitution_cost(0.8, 0.1) < 0.0);
    }

    #[test]
    fn an_alternate_no_nearer_than_the_typed_key_costs_at_least_the_edit() {
        assert!((substitution_cost(0.2, 0.2) - LAMBDA).abs() < f32::EPSILON);
        assert!(substitution_cost(0.1, 0.5) > LAMBDA);
    }

    #[test]
    fn two_edits_cost_more_than_one_at_equal_distance() {
        let one = substitution_cost(0.6, 0.2);
        let two = one + substitution_cost(0.6, 0.2);
        assert!(two > one);
    }

    #[test]
    fn a_candidate_reports_its_cost_with_the_sign_hosts_expect() {
        let mut candidate = CorrectionCandidate::default();
        candidate.fill("nhà", 1250, 1);
        assert!((candidate.cost() - 1.25).abs() < 0.001);
        assert!((candidate.touch_score() + 1.25).abs() < 0.001);
    }

    #[test]
    fn the_word_prior_floors_at_half_a_nat_for_an_unseen_word() {
        assert!((word_prior(0) - PRIOR_FLOOR).abs() < f32::EPSILON);
        assert!(word_prior(50) > word_prior(1));
    }

    #[test]
    fn milli_nats_round_trip_within_a_thousandth() {
        assert!((from_milli(to_milli(-3.25)) + 3.25).abs() < 0.001);
    }
}
