//! The ranked set: a fixed number of slots that keep the best candidates seen so
//! far, so the search never allocates for a word however many replays land.

use super::CorrectionCandidate;

/// Offer one candidate to the ranked set, keeping the cheapest `candidates.len()`
/// of them. `cost` is in milli-nats.
///
/// The same word is often reachable by more than one edit path, so a repeat text
/// updates the entry it matches instead of taking a second slot — and only when the
/// new path is strictly cheaper, so the first path to a cost keeps its edit count.
pub(crate) fn consider(
    candidates: &mut [CorrectionCandidate],
    len: &mut usize,
    text: &str,
    cost: i32,
    edits: u8,
) {
    if let Some(slot) = candidates[..*len].iter().position(|c| c.text == text) {
        if cost < candidates[slot].cost {
            candidates[slot].fill(text, cost, edits);
        }
        return;
    }
    if *len < candidates.len() {
        candidates[*len].fill(text, cost, edits);
        *len += 1;
        return;
    }
    let weakest = weakest_slot(&candidates[..*len]);
    if cost < candidates[weakest].cost {
        candidates[weakest].fill(text, cost, edits);
    }
}

/// The first of the slots that cost the most.
fn weakest_slot(candidates: &[CorrectionCandidate]) -> usize {
    let mut weakest = 0;
    for (i, candidate) in candidates.iter().enumerate() {
        if candidate.cost > candidates[weakest].cost {
            weakest = i;
        }
    }
    weakest
}

/// Order the filled slots cheapest first, which is the order the platform sees.
/// Stable, so candidates of equal cost keep the order the generators found them in.
pub(crate) fn rank(candidates: &mut [CorrectionCandidate]) {
    candidates.sort_by_key(|candidate| candidate.cost);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_ranked_set_keeps_the_best_and_drops_the_weakest() {
        let mut candidates = vec![CorrectionCandidate::default(); 2];
        let mut len = 0;
        consider(&mut candidates, &mut len, "nhà", 100, 1);
        consider(&mut candidates, &mut len, "nhá", 300, 1);
        consider(&mut candidates, &mut len, "nhã", 200, 1);
        rank(&mut candidates[..len]);
        assert_eq!(len, 2);
        assert_eq!(candidates[0].text(), "nhà");
        assert_eq!(candidates[1].text(), "nhã");
    }

    #[test]
    fn the_same_word_reached_twice_keeps_its_lower_cost() {
        let mut candidates = vec![CorrectionCandidate::default(); 4];
        let mut len = 0;
        consider(&mut candidates, &mut len, "đường", 400, 2);
        consider(&mut candidates, &mut len, "đường", 120, 1);
        consider(&mut candidates, &mut len, "đường", 120, 2);
        assert_eq!(len, 1);
        assert_eq!(candidates[0].cost, 120);
        assert_eq!(candidates[0].edits(), 1, "an equal cost does not replace");
    }

    #[test]
    fn equal_costs_keep_the_order_they_arrived_in() {
        let mut candidates = vec![CorrectionCandidate::default(); 4];
        let mut len = 0;
        consider(&mut candidates, &mut len, "nhá", 200, 1);
        consider(&mut candidates, &mut len, "nhà", 200, 1);
        consider(&mut candidates, &mut len, "nha", 100, 1);
        rank(&mut candidates[..len]);
        let texts: Vec<_> = candidates[..len].iter().map(|c| c.text()).collect();
        assert_eq!(texts, ["nha", "nhá", "nhà"]);
    }
}
