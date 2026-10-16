//! The ranked set: a fixed number of slots that keep the best candidates seen so
//! far, so the search never allocates for a word however many replays land.

use super::CorrectionCandidate;

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
