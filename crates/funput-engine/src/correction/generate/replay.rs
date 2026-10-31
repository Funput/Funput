//! The replay: typing a variant into the scratch session to see what it composes to.

use crate::EngineConfig;
use crate::compose::pipeline;
use crate::model::Session;

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
pub(super) fn replay(scratch: &mut Session, word: &[char]) -> bool {
    scratch.clear();
    for &key in word {
        if scratch.buffer.is_empty() && key.is_ascii_digit() {
            return false;
        }
        scratch.push_key(key);
        pipeline::process(scratch, key, key, false);
    }
    scratch
        .config
        .syllable_rules
        .is_complete_syllable(&scratch.buffer)
}

/// Point the scratch session at the user's grammar, with everything that rewrites a
/// buffer switched off.
///
/// English restore is the one that matters: left on, it collapses a candidate to its
/// raw keystrokes mid-replay — the same mechanism that makes the typed word look like
/// plain Latin at the boundary — and the syllable check would then judge keystrokes
/// instead of a composition.
pub(super) fn prepare(scratch: &mut Session, config: &EngineConfig) {
    scratch.config.clone_from(config);
    scratch.config.smart_restore = false;
    scratch.config.eager_restore = false;
    scratch.config.restore_doubled_keys = false;
    scratch.config.restore_collisions = false;
    scratch.config.spell_check = false;
    scratch.config.auto_capitalize = false;
    scratch.config.typo_correction = false;
}
