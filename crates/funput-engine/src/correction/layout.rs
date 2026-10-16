//! A keyboard the engine can assume when the host reported nothing: which keys sit
//! next to which.
//!
//! The same staggered QWERTY grid `funput dev typos` types on, measured in key
//! pitches, with the digit row for VNI's tone and shape keys. It is the *fallback*
//! evidence for a key that arrived without a touch report — a desktop keyboard, a
//! pasted word, a host that only reports ambiguous touches — so every neighbour costs
//! the same flat substitution and nothing here claims to know where a finger landed.

use crate::correction::evidence::{MAX_NEIGHBOURS, Neighbours};
use crate::correction::score::SUBSTITUTE_STATIC;

/// Rows, each with the x offset of its first key and its y, in pitches.
const ROWS: [(&str, f32, f32); 4] = [
    ("1234567890", 0.0, 0.0),
    ("qwertyuiop", 0.0, 1.0),
    ("asdfghjkl", 0.25, 2.0),
    ("zxcvbnm", 0.75, 3.0),
];

/// How far apart two key centres may be for one to be a slip for the other. Takes
/// in the diagonal neighbours of the staggered rows (1.12 pitches) and leaves out
/// the next ones over (1.25).
const REACH: f32 = 1.2;

/// The keys within [`REACH`] of `key`, nearest first — and, at equal distance, in
/// grid order. The digit row counts only when `digits` is set (VNI). A key not on
/// the grid has no neighbours, and the neighbours of a capital are capitals.
pub(crate) fn neighbours(key: char, digits: bool) -> Neighbours {
    let mut found = Neighbours::NONE;
    let Some(centre) = centre(key.to_ascii_lowercase(), digits) else {
        return found;
    };
    let mut nearest = [('\0', f32::INFINITY); MAX_NEIGHBOURS];
    let mut len = 0;
    for (row, offset, y) in ROWS.iter().skip(usize::from(!digits)) {
        for (index, other) in row.chars().enumerate() {
            let distance = distance(centre, (index as f32 + offset, *y));
            if other == key.to_ascii_lowercase() || distance > REACH {
                continue;
            }
            insert(&mut nearest, &mut len, other, distance);
        }
    }
    for &(neighbour, _) in &nearest[..len] {
        let neighbour = if key.is_uppercase() {
            neighbour.to_ascii_uppercase()
        } else {
            neighbour
        };
        found.push(neighbour, SUBSTITUTE_STATIC);
    }
    found
}

/// Keep the `MAX_NEIGHBOURS` nearest, ordered: a new key goes after every key at
/// least as near, and the farthest falls off the end.
fn insert(nearest: &mut [(char, f32); MAX_NEIGHBOURS], len: &mut usize, key: char, distance: f32) {
    let mut slot = *len;
    while slot > 0 && nearest[slot - 1].1 > distance {
        slot -= 1;
    }
    if slot == MAX_NEIGHBOURS {
        return;
    }
    let last = (*len).min(MAX_NEIGHBOURS - 1);
    nearest.copy_within(slot..last, slot + 1);
    nearest[slot] = (key, distance);
    *len = (*len + 1).min(MAX_NEIGHBOURS);
}

fn centre(key: char, digits: bool) -> Option<(f32, f32)> {
    ROWS.iter()
        .skip(usize::from(!digits))
        .find_map(|(row, offset, y)| {
            let index = row.find(key)?;
            Some((index as f32 + offset, *y))
        })
}

fn distance(from: (f32, f32), to: (f32, f32)) -> f32 {
    ((from.0 - to.0).powi(2) + (from.1 - to.1).powi(2)).sqrt()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn keys(key: char, digits: bool) -> String {
        neighbours(key, digits).iter().map(|(key, _)| key).collect()
    }

    #[test]
    fn a_home_row_key_reaches_its_row_and_the_rows_above_and_below() {
        assert_eq!(keys('g', false), "fhtvb");
    }

    #[test]
    fn a_top_row_key_reaches_the_digits_only_for_vni() {
        assert_eq!(keys('i', false), "uok");
        // `9` sits a diagonal away, 1.41 pitches: out of reach.
        assert_eq!(keys('i', true), "8uok");
    }

    #[test]
    fn a_digit_reaches_its_neighbours_and_the_letter_below() {
        assert_eq!(keys('1', true), "2q");
        assert_eq!(keys('1', false), "", "digits are not keys in Telex");
    }

    #[test]
    fn a_key_off_the_grid_has_no_neighbours() {
        assert_eq!(keys('[', true), "");
        assert_eq!(keys(' ', true), "");
    }

    #[test]
    fn a_capital_gets_capital_neighbours_at_the_flat_cost() {
        let found = neighbours('G', false);
        assert_eq!(
            found.iter().map(|(key, _)| key).collect::<String>(),
            "FHTVB"
        );
        assert!(found.iter().all(|(_, cost)| cost == SUBSTITUTE_STATIC));
    }

    #[test]
    fn no_key_has_more_neighbours_than_the_array_holds() {
        for key in ('a'..='z').chain('0'..='9') {
            assert!(
                neighbours(key, true).iter().count() <= MAX_NEIGHBOURS,
                "{key}"
            );
        }
    }
}
