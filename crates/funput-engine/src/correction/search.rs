//! Candidate search: which neighbouring keys, had they been pressed instead, would
//! have made a real Vietnamese syllable.
//!
//! The search is a bounded replay. Every combination of up to [`MAX_EDITS`] key
//! substitutions is fed back through the composition pipeline, and a combination is
//! kept when what comes out is a complete syllable. That reuses the Telex/VNI rules
//! instead of restating them — there is no second grammar to keep in step.
//!
//! Cost is capped by construction: 10 keys × 3 alternates for one edit, plus
//! C(10,2) × 3 × 3 for two, is at most 435 replays — on a word that is already known
//! not to be Vietnamese, once per word boundary.

use funput_core::is_complete_syllable;

use crate::EngineConfig;
use crate::compose::pipeline;
use crate::correction::score::{self, CorrectionCandidate};
use crate::correction::state::CorrectionState;
use crate::correction::touch::{MAX_WORD_KEYS, from_q8};
use crate::model::Session;

/// How many keys may be wrong at once. Three mistyped keys in one syllable is a
/// different word, not a slip.
const MAX_EDITS: usize = 2;
/// Shortest word worth correcting: one letter carries no evidence either way.
const MIN_WORD_KEYS: usize = 2;

/// Fill the ranked candidate set for the word `keys` produced, and return how many
/// candidates were found.
pub(crate) fn find(
    state: &mut CorrectionState,
    config: &EngineConfig,
    keys: &str,
    shown: &str,
) -> usize {
    let count = keys.chars().count();
    if !(MIN_WORD_KEYS..=MAX_WORD_KEYS).contains(&count) {
        return 0;
    }
    let CorrectionState {
        touch,
        scratch,
        candidates,
        len,
        ..
    } = state;
    prepare(scratch, config);
    let entries = touch.entries();
    let base = touch.base_score();
    let mut word = ['\0'; MAX_WORD_KEYS];
    for (slot, key) in word.iter_mut().zip(keys.chars()) {
        *slot = key;
    }
    *len = 0;

    // One edit, then that edit paired with a later one. The nesting *is* MAX_EDITS;
    // raising it means another loop, which the replay ceiling above is sized for.
    const _: () = assert!(MAX_EDITS == 2);
    for first in 0..count {
        for alternate in entries[first].alternates() {
            let one = base + delta(entries[first].typed_distance, alternate.distance);
            word[first] = alternate.key;
            consider(scratch, candidates, len, &word[..count], shown, one, 1);
            for second in first + 1..count {
                for paired in entries[second].alternates() {
                    let two = one + delta(entries[second].typed_distance, paired.distance);
                    word[second] = paired.key;
                    consider(scratch, candidates, len, &word[..count], shown, two, 2);
                    word[second] = entries[second].typed;
                }
            }
            word[first] = entries[first].typed;
        }
    }
    score::rank(&mut candidates[..*len]);
    *len
}

fn delta(typed: u16, alternate: u16) -> f32 {
    score::edit_delta(from_q8(typed), from_q8(alternate))
}

fn consider(
    scratch: &mut Session,
    candidates: &mut [CorrectionCandidate],
    len: &mut usize,
    word: &[char],
    shown: &str,
    total: f32,
    edits: u8,
) {
    if !replay(scratch, word) || scratch.buffer == shown {
        return;
    }
    score::consider(
        candidates,
        len,
        &scratch.buffer,
        score::to_milli(total),
        edits,
    );
}

/// Type `word` into the scratch session from scratch, and answer whether it lands on
/// a real syllable.
///
/// The raw keys have to grow with the buffer, as `Engine::process_key` grows them.
///
/// A digit cannot open a word — in VNI the modifiers *are* digits — and the engine
/// never puts one there, so one here can only be a substitution. It sinks the whole
/// candidate rather than being skipped: skipped, the candidate describes a word one
/// letter short of what the screen would show, and a `t` whose neighbour is `5` turns
/// `trước` into `rước`.
fn replay(scratch: &mut Session, word: &[char]) -> bool {
    scratch.clear();
    for &key in word {
        if scratch.buffer.is_empty() && key.is_ascii_digit() {
            return false;
        }
        scratch.keys.push(key);
        pipeline::process(scratch, key, false);
    }
    is_complete_syllable(&scratch.buffer)
}

/// Point the scratch session at the user's grammar, with everything that rewrites a
/// buffer switched off.
///
/// English restore is the one that matters: left on, it collapses a candidate to its
/// raw keystrokes mid-replay — the same mechanism that makes the typed word look like
/// plain Latin at the boundary — and the syllable check would then judge keystrokes
/// instead of a composition.
fn prepare(scratch: &mut Session, config: &EngineConfig) {
    scratch.config.clone_from(config);
    scratch.config.smart_restore = false;
    scratch.config.eager_restore = false;
    scratch.config.spell_check = false;
    scratch.config.auto_capitalize = false;
    scratch.config.typo_correction = false;
}
