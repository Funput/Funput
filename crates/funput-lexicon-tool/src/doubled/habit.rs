//! The doubled-key habit: pressing a key twice to get one plain letter back
//! (`tesst` → `test`, `off` → `of`, `ww` → `w` in Telex nâng cao).
//!
//! The habit can only ever drop a key that repeats the one before it. A revert that
//! loses a key anywhere else (`markers` → `makers`: the `r` after `ma` became the
//! tone, the second `r` cancelled it) is not something anyone types on purpose, so
//! the word that comes out cannot be what the user meant.

/// Whether `form` is `word` with one key dropped from some of its doubled keys: at
/// most one per run, since a revert takes one key (`sss` → `ss`, never `s`).
pub(crate) fn collapses_doubled_keys(word: &str, form: &str) -> bool {
    let word = word.as_bytes();
    let mut form = form.bytes().peekable();
    let mut dropped: Option<usize> = None;
    for (index, &byte) in word.iter().enumerate() {
        let twin = index > 0 && word[index - 1] == byte;
        if form.peek() == Some(&byte) {
            form.next();
        } else if twin && !dropped.is_some_and(|last| last + 1 == index) {
            dropped = Some(index);
        } else {
            return false;
        }
    }
    dropped.is_some() && form.next().is_none()
}

/// `typed` with every key that reverted its own twin dropped: what a user doubling
/// keys for plain letters means by it. Equal to `typed` when no revert was a double.
pub(crate) fn meant(typed: &str, reverting: impl IntoIterator<Item = usize>) -> String {
    let bytes = typed.as_bytes();
    let doubled: Vec<usize> = reverting
        .into_iter()
        .filter(|&index| index > 0 && bytes.get(index) == bytes.get(index - 1))
        .collect();
    typed
        .char_indices()
        .filter(|(index, _)| !doubled.contains(index))
        .map(|(_, key)| key)
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_doubled_key_collapses() {
        assert!(collapses_doubled_keys("off", "of"));
        assert!(collapses_doubled_keys("ww", "w"));
        assert!(collapses_doubled_keys("possess", "poses"));
        assert!(collapses_doubled_keys("seemed", "semed"));
    }

    #[test]
    fn a_key_lost_elsewhere_is_no_habit() {
        assert!(!collapses_doubled_keys("markers", "makers"));
        assert!(!collapses_doubled_keys("casts", "cats"));
        assert!(!collapses_doubled_keys("duress", "dues"));
        assert!(!collapses_doubled_keys("off", "off"));
        assert!(!collapses_doubled_keys("off", "ff"));
        // A form with keys left over, or a run losing two keys, is no single revert.
        assert!(!collapses_doubled_keys("off", "ofx"));
        assert!(!collapses_doubled_keys("sss", "s"));
        assert!(collapses_doubled_keys("sss", "ss"));
        assert!(!collapses_doubled_keys("", ""));
    }

    #[test]
    fn what_a_doubled_key_meant() {
        assert_eq!(meant("pass", [3]), "pas");
        assert_eq!(meant("passw", [3]), "pasw");
        // `basis` reverts on its second `s`, which follows an `i`: no habit there.
        assert_eq!(meant("basis", [4]), "basis");
        assert_eq!(meant("mas", []), "mas");
    }
}
