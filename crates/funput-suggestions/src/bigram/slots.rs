//! Which few following words a word keeps, and how a newcomer gets in.
//!
//! Pure on the slot array: no allocation, no `words`, O(`FOLLOWER_SLOTS`). What
//! liveness a caller knows, it passes in.

use super::follower::{FOLLOWER_SLOTS, Follower};

/// After this many sightings of one context word, every count it holds is halved.
///
/// Two jobs in one constant. It bounds `uses`, which can never exceed `seen`, so
/// the `u16` counts cannot overflow. And it is what lets a habit change: the
/// challenge rule below removes one point at a time, so a slot that got far
/// enough ahead would be unreachable forever without something that decays it.
///
/// Not in `SuggestionConfig` yet — the value is a guess until there is a reader
/// to calibrate it against.
pub(crate) const AGING_AFTER: u16 = 256;

pub(crate) fn record(
    followers: &mut [Follower; FOLLOWER_SLOTS],
    seen: &mut u16,
    dead: [bool; FOLLOWER_SLOTS],
    next: Follower,
) {
    *seen = seen.saturating_add(1);
    admit(followers, dead, next);
    if *seen >= AGING_AFTER {
        age(followers, seen);
    }
}

fn admit(followers: &mut [Follower; FOLLOWER_SLOTS], dead: [bool; FOLLOWER_SLOTS], next: Follower) {
    if let Some(index) = (0..FOLLOWER_SLOTS).find(|&index| followers[index].points_at(next)) {
        followers[index].uses = followers[index].uses.saturating_add(1);
        return;
    }
    if let Some(index) =
        (0..FOLLOWER_SLOTS).find(|&index| followers[index].is_free() || dead[index])
    {
        followers[index] = next;
        return;
    }

    // Every slot is taken by a living word, so the newcomer has to buy its way
    // in: the weakest pays a point for the challenge and only yields once it has
    // nothing left. A one-off typo never gets in; a pair typed a few times does.
    // Without this the first four words to follow a context would hold it for
    // good, and "xin" would answer with four typos forever.
    let weakest = (0..FOLLOWER_SLOTS)
        .min_by_key(|&index| followers[index].uses)
        .unwrap_or(0);
    followers[weakest].uses -= 1;
    if followers[weakest].is_free() {
        followers[weakest] = next;
    }
}

fn age(followers: &mut [Follower; FOLLOWER_SLOTS], seen: &mut u16) {
    *seen /= 2;
    for slot in followers {
        slot.uses /= 2;
        if slot.is_free() {
            // Halved out of existence: a word seen once here is noise, and its
            // slot is worth more to whatever comes next.
            *slot = Follower::EMPTY;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const ALIVE: [bool; FOLLOWER_SLOTS] = [false; FOLLOWER_SLOTS];

    fn follower(word: u32, uses: u16) -> Follower {
        Follower {
            word,
            generation: 0,
            uses,
        }
    }

    fn set(counts: [u16; FOLLOWER_SLOTS]) -> [Follower; FOLLOWER_SLOTS] {
        std::array::from_fn(|index| follower(index as u32, counts[index]))
    }

    #[test]
    fn a_repeat_counts_up_rather_than_taking_a_second_slot() {
        let mut followers = set([3, 2, 1, 1]);
        let mut seen = 7;
        record(&mut followers, &mut seen, ALIVE, follower(1, 1));
        assert_eq!(followers[1].uses, 3);
        assert_eq!(seen, 8);
    }

    #[test]
    fn a_free_slot_takes_the_newcomer_outright() {
        let mut followers = set([3, 0, 0, 0]);
        let mut seen = 3;
        record(&mut followers, &mut seen, ALIVE, follower(9, 1));
        assert_eq!(followers[1].word, 9);
        assert_eq!(followers[1].uses, 1);
    }

    #[test]
    fn a_dead_slot_is_reused_before_anyone_is_charged() {
        let mut followers = set([3, 2, 2, 2]);
        let mut seen = 9;
        let mut dead = ALIVE;
        dead[2] = true;
        record(&mut followers, &mut seen, dead, follower(9, 1));
        assert_eq!(followers[2].word, 9);
        assert_eq!(
            [followers[0].uses, followers[1].uses, followers[3].uses],
            [3, 2, 2],
            "nobody should pay while a dead slot is going spare"
        );
    }

    #[test]
    fn aging_halves_every_count_and_drops_the_one_offs() {
        let mut followers = set([8, 5, 2, 1]);
        let mut seen = AGING_AFTER - 1;
        record(&mut followers, &mut seen, ALIVE, follower(0, 1));

        assert_eq!(seen, AGING_AFTER / 2);
        assert_eq!(
            [followers[0].uses, followers[1].uses, followers[2].uses],
            [4, 2, 1]
        );
        assert!(
            followers[3].is_free(),
            "a single sighting should not survive an aging pass"
        );
    }

    #[test]
    fn aging_keeps_an_entrenched_slot_reachable() {
        // The challenge rule removes one point per sighting, so without decay a
        // set that got this far ahead could never be displaced at all.
        let mut followers = set([250, 250, 250, 250]);
        let mut seen = AGING_AFTER - 1;
        record(&mut followers, &mut seen, ALIVE, follower(9, 1));

        assert!(followers.iter().all(|slot| slot.uses <= 125));
    }

    #[test]
    fn a_full_set_charges_the_weakest_and_only_yields_at_zero() {
        let mut followers = set([3, 2, 2, 2]);
        let mut seen = 9;
        record(&mut followers, &mut seen, ALIVE, follower(9, 1));
        assert_eq!(followers[1].uses, 1, "the weakest pays for the challenge");
        assert!(
            followers.iter().all(|slot| slot.word != 9),
            "one sighting must not be enough to displace an established pair"
        );

        record(&mut followers, &mut seen, ALIVE, follower(9, 1));
        assert_eq!(followers[1].word, 9, "persistence eventually wins the slot");
        assert_eq!(followers[1].uses, 1);
    }
}
