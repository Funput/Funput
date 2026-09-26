//! Naming the winner: the platform's prior folded into each candidate, and the word
//! exactly as typed standing as the rival every candidate has to beat.

use super::{CorrectionCandidate, MARGIN, word_prior};

/// What believing the user meant a word that is not Vietnamese costs, in nats.
///
/// The word as typed is a hypothesis too. Without it in the comparison a lone
/// candidate won by default, however squarely the finger sat on the key it hit — so
/// `ko`, a name, anything deliberate that happens to be one key from a syllable, was
/// rewritten. With it, a candidate has to beat the typed word by [`MARGIN`], and a
/// touch near the centre of the key it registered is evidence *for* the typed word.
///
/// Measured with `funput dev typos` at the finger spread read off a device (0.174),
/// 40 seeds of `--keep` (words typed on purpose) against 8 seeds of Viet74K:
///
/// | cost | deliberate words rewritten, VNI / Telex | slips repaired, VNI / Telex |
/// |---|---|---|
/// | none (a lone candidate always won) | 24.5% / 26.4% | 66.9% / 51.7% |
/// | **4.0** | **4.5% / 5.1%** | **66.5% / 51.5%** |
/// | 3.0 | 0.3% / 0.5% | 64.3% / 50.2% |
/// | 2.5 | 0.1% / 0.1% | 45.8% / 37.1% |
///
/// That sweep models a finger scattered around the key it aimed at, so every slip
/// lands next to the edge it crossed, and 3.0 looked nearly free. On a device it was
/// not: at 3.0 `abh` only became `anh` for a touch in the outer 15% of `b`, and a real
/// mistake often lands well inside the wrong key. 4.0 trusts only a touch within about
/// 0.15 pitches of a key's centre. The deliberate words that still get through are the
/// keep list's job — `is_known_word` knows them by name, and a keyboard vetoes them.
pub(crate) const INVALID_COST: f32 = 4.0;

/// Why the ranking came out the way it did.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Choice {
    /// Apply the candidate at this index.
    Apply(usize),
    /// The two best candidates were too close to call.
    Ambiguous,
    /// No candidate beat the word as typed by enough to overrule the finger.
    AsTyped,
}

/// Rank `candidates` with the platform's `uses` and `allowed` (both parallel, both
/// read as "0" and "permitted" past their ends) against the typed word's score.
///
/// `None` when every candidate was refused.
pub(crate) fn choose(
    candidates: &[CorrectionCandidate],
    uses: &[u32],
    allowed: &[bool],
    as_typed: f32,
) -> Option<Choice> {
    let mut best: Option<(usize, f32)> = None;
    let mut runner_up = f32::NEG_INFINITY;
    for (i, candidate) in candidates.iter().enumerate() {
        let score = candidate.touch_score() + word_prior(uses.get(i).copied().unwrap_or(0));
        // A refused candidate cannot win, but it still competes for the margin.
        // Measured: letting it drop out entirely is what makes an *incomplete*
        // dictionary dangerous rather than merely unhelpful — the word the host does
        // not know is often the right one, and without it in the comparison a common
        // wrong word wins uncontested. Kept in, it suppresses that word instead, so a
        // dictionary that knows too little corrects less rather than corrects badly.
        if !allowed.get(i).copied().unwrap_or(true) {
            runner_up = runner_up.max(score);
            continue;
        }
        match best {
            Some((_, leader)) if leader >= score => runner_up = runner_up.max(score),
            Some((_, leader)) => {
                runner_up = runner_up.max(leader);
                best = Some((i, score));
            }
            None => best = Some((i, score)),
        }
    }
    let (index, top) = best?;
    if top - runner_up.max(as_typed) >= MARGIN {
        return Some(Choice::Apply(index));
    }
    Some(if as_typed >= runner_up {
        Choice::AsTyped
    } else {
        Choice::Ambiguous
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::correction::score::{consider, to_milli};

    fn ranked(scores: &[f32]) -> Vec<CorrectionCandidate> {
        let mut candidates = vec![CorrectionCandidate::default(); scores.len()];
        let mut len = 0;
        for (i, &score) in scores.iter().enumerate() {
            consider(
                &mut candidates,
                &mut len,
                &i.to_string(),
                to_milli(score),
                1,
            );
        }
        candidates
    }

    #[test]
    fn a_pair_inside_the_margin_is_ambiguous() {
        let candidates = ranked(&[-2.0, -2.5]);
        assert_eq!(
            choose(&candidates, &[], &[], f32::NEG_INFINITY),
            Some(Choice::Ambiguous)
        );
        let candidates = ranked(&[-2.0, -3.5]);
        assert_eq!(
            choose(&candidates, &[], &[], f32::NEG_INFINITY),
            Some(Choice::Apply(0))
        );
    }

    #[test]
    fn a_lone_candidate_still_has_to_beat_the_word_as_typed() {
        let candidates = ranked(&[-3.0]);
        assert_eq!(choose(&candidates, &[], &[], -2.0), Some(Choice::AsTyped));
        assert_eq!(choose(&candidates, &[], &[], -5.0), Some(Choice::Apply(0)));
    }

    #[test]
    fn nothing_to_choose_when_every_candidate_is_refused() {
        let candidates = ranked(&[-1.0]);
        assert_eq!(choose(&candidates, &[], &[false], f32::NEG_INFINITY), None);
    }
}
