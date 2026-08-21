//! Storing an imported trigger the way the engine will look for it.

/// Store the trigger the way the engine expects to find it.
///
/// `funput_engine`'s `classify_case` folds a typed word to lowercase before looking
/// it up whenever its letters form a clean pattern, so `vn`, `Vn` and `VN` all reach
/// a trigger stored as `vn` and re-case the expansion to match. A trigger stored in
/// any other casing is only ever found by an exact match — so folding is what makes
/// an imported `VN` usable, and *not* folding is what keeps a deliberate `iOS` from
/// becoming unreachable.
pub(super) fn normalize_trigger(trigger: &str) -> String {
    if folds_to_lowercase(trigger) {
        trigger.to_lowercase()
    } else {
        trigger.to_string()
    }
}

/// Mirrors `classify_case` returning `Some`: letters only, digits and punctuation
/// ignored; first letter lowercase with the rest lowercase, or first letter
/// uppercase with the rest all one case. A trigger with no letters folds to itself.
fn folds_to_lowercase(trigger: &str) -> bool {
    let mut letters = trigger.chars().filter(|c| c.is_alphabetic());
    let Some(first) = letters.next() else {
        return true;
    };
    let rest: Vec<char> = letters.collect();
    if first.is_lowercase() {
        rest.iter().all(|c| c.is_lowercase())
    } else {
        rest.iter().all(|c| c.is_uppercase()) || rest.iter().all(|c| c.is_lowercase())
    }
}
