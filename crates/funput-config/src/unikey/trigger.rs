//! Storing an imported trigger the way the engine will look for it.

use funput_core::shortcut::stored_case;

/// Store the trigger the way the engine expects to find it.
///
/// The engine folds a typed word to lowercase before looking it up, so `vn`, `Vn`
/// and `VN` all reach a trigger stored as `vn` and re-case the expansion to match.
/// A trigger stored in a cleanly cased spelling folds the same way here, which is
/// what makes an imported `VN` usable. One stored in a mixed spelling
/// ([`stored_case`] gives it no case) is only ever found by an exact match, so
/// *not* folding it is what keeps a deliberate `iOS` or `BBh` reachable.
pub(super) fn normalize_trigger(trigger: &str) -> String {
    if stored_case(trigger).is_some() {
        trigger.to_lowercase()
    } else {
        trigger.to_string()
    }
}
