//! Tone mark placement — modern Vietnamese reposition rules.
//!
//! Hot path: runs on every normal keystroke, so the cluster analysis is a
//! single allocation-free pass; only an actual reposition builds a `String`.

use super::cluster::{modern_open_pair_takes_second, scan_cluster, tone_offset_in_cluster};

use crate::ToneStyle;
use crate::unicode::marks::{apply_tone_to_vowel, is_vowel, tone_on_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, apply_shape};

/// Char index where a tone mark should be placed, for the given placement `style`.
pub(crate) fn tone_vowel_index(buffer: &str, style: ToneStyle) -> Option<usize> {
    let cluster = scan_cluster(buffer)?;

    // A vowel carrying mũ/móc/trần (â ê ô ơ ư ă) takes the tone. For `ươ` (two
    // horned vowels) the tone sits on the second one (`ơ`): trường, được, rượu.
    if let Some(i) = cluster.last_shaped {
        return Some(i);
    }

    // "Kiểu mới": an open `oa`/`oe`/`uy` takes the tone on the second vowel.
    if style == ToneStyle::Modern
        && cluster.len == 2
        && !cluster.has_coda
        && let Some(second) = cluster.second
        && modern_open_pair_takes_second(cluster.first, second)
    {
        return Some(cluster.start + 1);
    }

    let offset = tone_offset_in_cluster(cluster.len, cluster.has_coda);
    Some(cluster.start + offset.min(cluster.len - 1))
}

/// Vowel character used when applying a tone (handles `ie`/`gie` → tonal base `ê`).
///
/// Plain `e` preceded by `i` forms the rising diphthong written with a circumflex
/// (`viết`, `giết`), so the tone lands on `ê`. This covers both the `i` nucleus of
/// `viet` and the `i` that is part of the `gi` onset of `giet`.
pub(crate) fn tone_target_vowel(buffer: &str, vowel_idx: usize) -> Option<char> {
    // Chars at vowel_idx - 1, vowel_idx, vowel_idx + 1, in one pass.
    let (mut prev, mut vowel, mut next) = (None, None, None);
    for (i, ch) in buffer.chars().enumerate() {
        if i + 1 == vowel_idx {
            prev = Some(ch);
        } else if i == vowel_idx {
            vowel = Some(ch);
        } else if i == vowel_idx + 1 {
            next = Some(ch);
            break;
        }
    }

    let vowel = vowel?;
    let stem = vowel_stem(vowel)?;
    if !stem.eq_ignore_ascii_case(&'e') {
        return Some(vowel);
    }
    // The preceding `i` may already carry a tone (`vịe` + `t` → `việt`), so compare
    // its stem, not the raw char.
    let prev_is_i = prev
        .and_then(vowel_stem)
        .is_some_and(|s| s.eq_ignore_ascii_case(&'i'));
    if !prev_is_i {
        return Some(vowel);
    }

    // The `iê` diphthong exists only closed (`iêt`/`iên`/`iêm`/…) or as the open
    // triphthong `iêu`; open `ie`/`ieo` do not (open /iə/ is written `ia`). So promote
    // only when `e` is followed by a coda consonant or the glide `u` — otherwise keep
    // plain `e`. This fixes the gi-onset words `gié`/`giẻ`/`giẹo` (previously
    // `giế`/`giể`/`giếo`) while leaving `viết`/`giết`/`chiếu` untouched.
    let promote = match next {
        None => false, // open `ie` → keep `e` (`gié`)
        Some(c) if is_vowel(c) => vowel_stem(c) // `ieu` promotes, `ieo` does not
            .is_some_and(|s| s.eq_ignore_ascii_case(&'u')),
        Some(_) => true, // coda consonant → `iê`
    };
    if !promote {
        return Some(vowel);
    }

    // `apply_shape` already preserves case (`e` → `ê`, `E` → `Ê`).
    apply_shape(stem, VowelShape::Circumflex)
}

/// If a tone exists on the wrong vowel, move it to the correct position for `style`.
///
/// Only the *position* is revisited, never the tonal base of a tone that already
/// sits right. That is deliberate: where the user put the tone key disambiguates
/// the two `gi` + `e` + coda rhymes that structure alone cannot separate — before
/// the coda keeps plain `e` (`giefm` → `gièm`), after it promotes to `ê`
/// (`giemf` → `giềm`). Both are real words.
pub(crate) fn reposition_existing_tone(buffer: &str, style: ToneStyle) -> Option<String> {
    // Cheap scan first: most keystrokes carry no tone yet, and this runs on
    // every normal key — bail out without any cluster analysis or allocation.
    let (current, current_ch, tone) = buffer
        .chars()
        .enumerate()
        .find_map(|(i, ch)| tone_on_vowel(ch).map(|t| (i, ch, t)))?;

    let desired = tone_vowel_index(buffer, style)?;
    if current == desired {
        return None;
    }

    let old_stem = vowel_stem(current_ch)?;
    let desired_ch = buffer.chars().nth(desired)?;
    let tone_base = tone_target_vowel(buffer, desired).unwrap_or(desired_ch);
    let toned = apply_tone_to_vowel(tone_base, tone)?;

    let mut moved = String::with_capacity(buffer.len() + toned.len_utf8());
    for (i, ch) in buffer.chars().enumerate() {
        moved.push(match i {
            _ if i == current => old_stem,
            _ if i == desired => toned,
            _ => ch,
        });
    }
    Some(moved)
}
