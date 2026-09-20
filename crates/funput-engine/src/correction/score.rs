//! The candidate type and the arithmetic that ranks candidates.
//!
//! The touch model is Gaussian: a finger landing `d` key-pitches from a key's centre
//! makes that key `exp(-d² / 2σ²)` likely, so in log space every key contributes
//! `-d² / 2σ²` and a word is the sum of its keys. Swapping one key for a neighbour
//! trades that key's term for the neighbour's and pays a flat `λ`, which stops a
//! two-key guess from beating a one-key guess on rounding noise alone.
//!
//! Scores are kept as milli-nats (`i32`) rather than `f32`: a candidate is reachable
//! from `Session`, which derives `Eq`.

/// Touch spread in key pitches — how far a finger typically lands from centre.
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

/// One way the word could have been meant, with the touch evidence behind it.
///
/// The score is the touch evidence *only*. How likely the word is — the user's own
/// use counts, an English lexicon — lives outside this crate, so the platform adds
/// its prior before choosing (see [`crate::Engine::choose_correction`]).
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct CorrectionCandidate {
    text: String,
    score: i32,
    edits: u8,
}

impl CorrectionCandidate {
    /// The corrected word, ready to replace what is on screen.
    pub fn text(&self) -> &str {
        &self.text
    }

    /// Log-likelihood of this word given where the fingers landed, in nats. Always
    /// negative; closer to zero is better.
    pub fn touch_score(&self) -> f32 {
        from_milli(self.score)
    }

    /// How many keys had to be substituted to reach this word.
    pub fn edits(&self) -> usize {
        usize::from(self.edits)
    }

    /// Refill in place so the string capacity survives into the next word.
    fn fill(&mut self, text: &str, score: i32, edits: u8) {
        self.text.clear();
        self.text.push_str(text);
        self.score = score;
        self.edits = edits;
    }
}

/// log P(touch | key) for one key, in nats: zero for a touch dead on the centre.
fn key_term(distance: f32) -> f32 {
    -(distance * distance) / (2.0 * SIGMA * SIGMA)
}

/// What swapping one key for an alternate does to a word's score.
pub(crate) fn edit_delta(typed_distance: f32, alternate_distance: f32) -> f32 {
    key_term(alternate_distance) - key_term(typed_distance) - LAMBDA
}

/// The score of the word exactly as it was typed — the baseline every candidate
/// starts from.
pub(crate) fn base_score(distances: impl Iterator<Item = f32>) -> f32 {
    distances.map(key_term).sum()
}

/// How much a word's use count is worth, in nats. `ln(1 + uses)` grows slowly enough
/// that a familiar word cannot outvote two edits' worth of touch evidence.
pub(crate) fn word_prior(uses: u32) -> f32 {
    (1.0 + uses as f32).ln() + PRIOR_FLOOR
}

/// Whether the runner-up is close enough that the winner should be offered rather
/// than applied.
pub(crate) fn is_ambiguous(best: f32, runner_up: f32) -> bool {
    best - runner_up < MARGIN
}

pub(crate) fn to_milli(score: f32) -> i32 {
    (score * 1000.0) as i32
}

pub(crate) fn from_milli(score: i32) -> f32 {
    score as f32 / 1000.0
}

/// Offer one candidate to the ranked set, keeping the best `candidates.len()` of them.
///
/// The same word is often reachable by more than one edit path, so a repeat text
/// updates the entry it matches instead of taking a second slot.
pub(crate) fn consider(
    candidates: &mut [CorrectionCandidate],
    len: &mut usize,
    text: &str,
    score: i32,
    edits: u8,
) {
    if let Some(slot) = candidates[..*len].iter().position(|c| c.text == text) {
        if score > candidates[slot].score {
            candidates[slot].fill(text, score, edits);
        }
        return;
    }
    if *len < candidates.len() {
        candidates[*len].fill(text, score, edits);
        *len += 1;
        return;
    }
    let weakest = weakest_slot(&candidates[..*len]);
    if score > candidates[weakest].score {
        candidates[weakest].fill(text, score, edits);
    }
}

fn weakest_slot(candidates: &[CorrectionCandidate]) -> usize {
    let mut weakest = 0;
    for (i, candidate) in candidates.iter().enumerate() {
        if candidate.score < candidates[weakest].score {
            weakest = i;
        }
    }
    weakest
}

/// Order the filled slots best first, which is the order the platform sees.
pub(crate) fn rank(candidates: &mut [CorrectionCandidate]) {
    candidates.sort_by_key(|candidate| std::cmp::Reverse(candidate.score));
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_nearer_alternate_outscores_the_key_that_was_typed() {
        // The finger landed most of a key away from what it hit, and a hair from
        // its neighbour: the swap is worth more than the λ it costs.
        assert!(edit_delta(0.8, 0.1) > 0.0);
    }

    #[test]
    fn an_alternate_no_nearer_than_the_typed_key_is_never_worth_the_edit() {
        assert!(edit_delta(0.2, 0.2) < 0.0);
    }

    #[test]
    fn two_edits_are_penalised_more_than_one_at_equal_distance() {
        let one = edit_delta(0.6, 0.2);
        let two = one + edit_delta(0.6, 0.2);
        assert!(two < one);
    }

    #[test]
    fn a_pair_inside_the_margin_is_called_ambiguous() {
        assert!(is_ambiguous(-2.0, -2.5));
        assert!(!is_ambiguous(-2.0, -3.5));
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

    #[test]
    fn the_ranked_set_keeps_the_best_and_drops_the_weakest() {
        let mut candidates = vec![CorrectionCandidate::default(); 2];
        let mut len = 0;
        consider(&mut candidates, &mut len, "nhà", -100, 1);
        consider(&mut candidates, &mut len, "nhá", -300, 1);
        consider(&mut candidates, &mut len, "nhã", -200, 1);
        rank(&mut candidates[..len]);
        assert_eq!(len, 2);
        assert_eq!(candidates[0].text(), "nhà");
        assert_eq!(candidates[1].text(), "nhã");
    }

    #[test]
    fn the_same_word_reached_twice_keeps_its_better_score() {
        let mut candidates = vec![CorrectionCandidate::default(); 4];
        let mut len = 0;
        consider(&mut candidates, &mut len, "đường", -400, 2);
        consider(&mut candidates, &mut len, "đường", -120, 1);
        assert_eq!(len, 1);
        assert_eq!(candidates[0].score, -120);
        assert_eq!(candidates[0].edits(), 1);
    }
}
