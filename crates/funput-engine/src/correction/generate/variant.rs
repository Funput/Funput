//! One hypothesis about the raw keys, and the three ways of reshaping it.

use crate::correction::touch::MAX_WORD_KEYS;

/// The raw keys of one hypothesis. One slot more than the longest word, so an
/// insertion fits.
#[derive(Debug, Clone, Copy)]
pub(crate) struct Variant {
    pub(crate) keys: [char; MAX_WORD_KEYS + 1],
    /// For each position, the typed key whose touch evidence it inherits — `None`
    /// for a key no finger pressed. A substitution keeps it, a deletion shifts the
    /// tail, a transposition swaps two, an insertion has nothing to inherit.
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

    /// The word without the key at `at`. The capital stays at the front: dropping
    /// the `N` of `Nhad` reads as `Had`, not `had`.
    pub(crate) fn deleted(&self, at: usize) -> Self {
        let mut variant = *self;
        variant.keys.copy_within(at + 1..self.len, at);
        variant.origin.copy_within(at + 1..self.len, at);
        variant.len = self.len - 1;
        if at == 0 {
            variant.keys[0] = recase_like(variant.keys[0], self.keys[0]);
        }
        variant
    }

    /// The word with the keys at `at` and `at + 1` the other way round, each still
    /// carrying its own evidence. The case stays with the position: the shift was
    /// held for the first letter of `Nhad`, not for the `N`, so `Hnad` is the reading.
    pub(crate) fn transposed(&self, at: usize) -> Self {
        let mut variant = *self;
        variant.keys.swap(at, at + 1);
        variant.origin.swap(at, at + 1);
        for position in [at, at + 1] {
            variant.keys[position] = recase_like(variant.keys[position], self.keys[position]);
        }
        variant
    }

    /// The word with `key` slipped in before position `at` (`at == len` appends),
    /// in the case of the key it lands next to. The caller keeps `len` under the
    /// cap. In front, the new key takes the capital and the old first key loses it.
    pub(crate) fn inserted(&self, at: usize, key: char) -> Self {
        let mut variant = *self;
        variant.keys.copy_within(at..self.len, at + 1);
        variant.origin.copy_within(at..self.len, at + 1);
        // The first key's case is the word's, not the letter's: a key going in
        // right after it follows the rest of the word instead.
        let beside = match at {
            0 => self.keys[0],
            1 => self.keys[1],
            _ => self.keys[at - 1],
        };
        variant.keys[at] = recase_like(key, beside);
        variant.origin[at] = None;
        variant.len = self.len + 1;
        if at == 0 {
            variant.keys[1] = variant.keys[1].to_ascii_lowercase();
        }
        variant
    }
}

/// `key` in the case of `like`.
fn recase_like(key: char, like: char) -> char {
    if like.is_uppercase() {
        key.to_ascii_uppercase()
    } else {
        key.to_ascii_lowercase()
    }
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
    }

    #[test]
    fn a_deletion_shifts_the_tail_with_its_evidence() {
        let word = Variant::typed("chowii").deleted(5);
        assert_eq!(word.keys(), ['c', 'h', 'o', 'w', 'i']);
        let word = Variant::typed("chwoi").deleted(2);
        assert_eq!(word.keys(), ['c', 'h', 'o', 'i']);
        assert_eq!(&word.origin[..4], [Some(0), Some(1), Some(3), Some(4)]);
    }

    #[test]
    fn a_transposition_swaps_the_keys_and_their_evidence() {
        let word = Variant::typed("chwoi").transposed(2);
        assert_eq!(word.keys(), ['c', 'h', 'o', 'w', 'i']);
        assert_eq!(
            &word.origin[..5],
            [Some(0), Some(1), Some(3), Some(2), Some(4)]
        );
    }

    #[test]
    fn an_insertion_carries_no_evidence_and_moves_the_rest_along() {
        let word = Variant::typed("chwi").inserted(2, 'o');
        assert_eq!(word.keys(), ['c', 'h', 'o', 'w', 'i']);
        assert_eq!(
            &word.origin[..5],
            [Some(0), Some(1), None, Some(2), Some(3)]
        );
        let word = Variant::typed("ab").inserted(2, 'c');
        assert_eq!(word.keys(), ['a', 'b', 'c']);
    }
}
