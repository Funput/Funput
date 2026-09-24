use crate::composition::intent::has_pending;
use crate::input_method::{AdvancedAction, KeyAction, TelexShortcut};
use crate::orthography::glide::{self, Glide};
use crate::unicode::marks::is_vowel;

use super::classify_key;

/// Full Telex classifier. Ordinary Telex keeps its unchanged fast path.
pub(super) fn classify(buffer: &str, key: char) -> AdvancedAction {
    if has_pending(buffer) {
        return AdvancedAction::Standard(KeyAction::DeferredW);
    }
    match key {
        '[' => AdvancedAction::Shortcut(TelexShortcut::HornU),
        ']' => AdvancedAction::Shortcut(TelexShortcut::HornO),
        'w' | 'W' if leading_w(buffer) => AdvancedAction::Shortcut(TelexShortcut::LeadingW),
        'w' | 'W' if ends_with_w_after_vowel(buffer) => {
            AdvancedAction::Shortcut(TelexShortcut::RepeatedW)
        }
        _ => AdvancedAction::Standard(classify_key(buffer, key)),
    }
}

fn ends_with_w_after_vowel(buffer: &str) -> bool {
    buffer
        .chars()
        .last()
        .is_some_and(|ch| ch.eq_ignore_ascii_case(&'w'))
        && buffer.chars().any(is_vowel)
}

/// Full Telex `w` stands in for `ư` as long as the syllable has no nucleus for
/// it to modify: `w` → `ư`, and equally `th` + `w` → `thư`, `gi` + `w` → `giư`.
/// It also owns the undo of that `ư` (`thư` + `w` → `thw`), which is what keeps
/// a Latin run escapable — `swwap` → `swap`.
fn leading_w(buffer: &str) -> bool {
    onset_only(buffer) || horn_u_after_onset(buffer)
}

/// No nucleus yet: the buffer is empty or a bare onset.
///
/// A lone `q` is excluded — no Vietnamese syllable reads `qư`, `q` is always
/// followed by the `u` glide, so a `w` there is the ordinary trần/móc waiting on
/// the vowel behind it (`qwuangj` → `quặng`).
fn onset_only(buffer: &str) -> bool {
    !matches!(buffer, "q" | "Q") && lacks_nucleus(buffer)
}

/// An onset plus the single `ư` a leading `w` just produced — the undo target.
fn horn_u_after_onset(buffer: &str) -> bool {
    buffer.strip_suffix(['ư', 'Ư']).is_some_and(lacks_nucleus)
}

/// True when `onset` holds no vowel for a `w` to shape: a bare consonant
/// cluster, or the `gi` onset, whose `i` is the medial glide rather than the
/// nucleus (`giữ` is `gi` + `ư`). The glide match is tone-blind, so the `gĩ` a
/// tone typed before the nucleus leaves behind qualifies too. `qu` never does —
/// no syllable reads `quư`.
fn lacks_nucleus(onset: &str) -> bool {
    !onset.chars().any(is_vowel) || glide::in_onset(onset) == Some(Glide::Gi)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn recognizes_only_full_telex_extensions() {
        assert_eq!(
            classify("", 'w'),
            AdvancedAction::Shortcut(TelexShortcut::LeadingW)
        );
        assert_eq!(
            classify("t", '['),
            AdvancedAction::Shortcut(TelexShortcut::HornU)
        );
        assert_eq!(
            classify("m", ']'),
            AdvancedAction::Shortcut(TelexShortcut::HornO)
        );
        assert_eq!(
            classify("lw", '['),
            AdvancedAction::Standard(KeyAction::DeferredW)
        );
        assert_eq!(
            classify("a", 's'),
            AdvancedAction::Standard(classify_key("a", 's'))
        );
    }

    #[test]
    fn gi_glide_is_part_of_the_onset() {
        let leading = AdvancedAction::Shortcut(TelexShortcut::LeadingW);
        for buffer in ["gi", "Gi", "GI", "gĩ", "giư", "GIƯ"] {
            assert_eq!(classify(buffer, 'w'), leading, "{buffer}");
        }
        // `i` is the nucleus once a coda follows, and `qu` + `ư` is no syllable.
        for buffer in ["gin", "qu", "q"] {
            assert_eq!(
                classify(buffer, 'w'),
                AdvancedAction::Standard(classify_key(buffer, 'w')),
                "{buffer}"
            );
        }
    }
}
