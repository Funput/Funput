//! The two POD types typo correction carries across the C ABI.

use funput_engine::{CorrectionCandidate, KeyTouch, MAX_ALTERNATES};

use crate::abi;

/// Neighbouring keys a host may offer per touch.
///
/// Spelled out rather than aliased to `funput_engine::MAX_ALTERNATES`, because
/// cbindgen does not read the engine crate and would emit a `#define` naming a macro
/// the header never defines. The assertion below is what keeps the two in step.
pub const TOUCH_ALTERNATE_CAP: usize = 3;
const _: () = assert!(TOUCH_ALTERNATE_CAP == MAX_ALTERNATES);
/// Candidates a host can read back for one word.
pub const CORRECTION_CAP: usize = 8;
/// Codepoints carried per candidate — the same cap the suggestion bar uses, and far
/// past the longest Vietnamese syllable.
pub const CORRECTION_CHARS_CAP: usize = 32;

/// Where one touch landed, as the host measured it.
///
/// Parallel arrays rather than an array of `(key, distance)` structs, so Swift and
/// Kotlin see a flat layout with no padding to reason about. Distances are in key
/// pitches — the gap between two neighbouring key centres — so the model does not
/// care about screen size or keyboard height.
#[repr(C)]
#[derive(Clone, Copy)]
pub struct FunputKeyTouch {
    /// The key the host resolved this touch to, as a Unicode scalar.
    pub typed: u32,
    pub typed_distance: f32,
    /// Keys the same touch could have meant, nearest first.
    pub alternates: [u32; TOUCH_ALTERNATE_CAP],
    pub distances: [f32; TOUCH_ALTERNATE_CAP],
    /// How many entries of `alternates` are set; anything past `TOUCH_ALTERNATE_CAP`
    /// is ignored.
    pub alternate_count: u32,
}

impl FunputKeyTouch {
    /// `None` when `typed` is not a Unicode scalar — the one thing the host can get
    /// wrong that the engine cannot work around.
    pub(crate) fn decode(&self) -> Option<KeyTouch> {
        let mut touch = KeyTouch::new(char::from_u32(self.typed)?, self.typed_distance);
        let count = (self.alternate_count as usize).min(TOUCH_ALTERNATE_CAP);
        for (key, distance) in self.alternates[..count]
            .iter()
            .zip(&self.distances[..count])
        {
            if let Some(key) = char::from_u32(*key) {
                touch = touch.with_alternate(key, *distance);
            }
        }
        Some(touch)
    }
}

/// One word the touch evidence can still reach.
#[repr(C)]
#[derive(Clone, Copy)]
pub struct FunputCorrectionCandidate {
    /// Codepoints of the corrected word in `chars`.
    pub count: u32,
    pub chars: [u32; CORRECTION_CHARS_CAP],
    /// Touch evidence only, in nats; always negative, closer to zero is better. The
    /// host adds its own word prior — or lets `funput_engine_choose_correction` do it.
    pub touch_score: f32,
    /// How many keys had to be substituted to reach this word.
    pub edits: u32,
}

impl FunputCorrectionCandidate {
    pub(crate) fn from_candidate(candidate: &CorrectionCandidate) -> Self {
        let mut encoded = Self::default();
        encoded.count = abi::copy_codepoints(&mut encoded.chars, candidate.text().chars()) as u32;
        encoded.touch_score = candidate.touch_score();
        encoded.edits = candidate.edits() as u32;
        encoded
    }
}

impl Default for FunputCorrectionCandidate {
    fn default() -> Self {
        Self {
            count: 0,
            chars: [0; CORRECTION_CHARS_CAP],
            touch_score: 0.0,
            edits: 0,
        }
    }
}
