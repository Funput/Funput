//! Where a context lands in the table.

use crate::types::WordRef;

/// The probe start for `key` in a table of `len` entries, a power of two.
///
/// Multiplicative hashing over each word's slot and generation, read from the
/// *top* bits: those are the ones every input bit reaches, generation included,
/// so a reused slot's new tenant starts probing away from its old tenant's
/// entries. The low bits of a product only ever see the low bits of its input.
pub(crate) fn start<const K: usize>(key: &[WordRef; K], len: usize) -> usize {
    const SEED: u64 = 0x9e37_79b9_7f4a_7c15;
    let hash = key.iter().fold(0u64, |hash, word| {
        let value = u64::from(word.index) | (u64::from(word.generation) << 32);
        (hash.rotate_left(29) ^ value).wrapping_mul(SEED)
    });
    (hash >> (64 - len.trailing_zeros())) as usize
}
