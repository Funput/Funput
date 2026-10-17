//! The faults a fast typist makes that a wandering finger does not.
//!
//! Touch noise only ever swaps a key for its neighbour. Real fast typing also
//! doubles a key (`chơii`), drops one, swaps two (`chwoi`) and lands a tone or
//! shape key where none belongs (`chowis`). This file does one of those to a
//! word's canonical key sequence *before* the finger wanders, so the harness can
//! say, per kind of fault, how much of it the engine repairs.
//!
//! At most one fault per word: the question is which single faults are repaired,
//! not how deep the hole can get.

use funput_core::{InputMethod, KeyRole, key_role};

use super::noise::Rng;

/// A single editing fault applied to a key sequence.
#[derive(Clone, Copy, PartialEq, Eq, Debug, clap::ValueEnum)]
pub(crate) enum Fault {
    /// A key pressed twice (`chowii`).
    Duplicate,
    /// A key not pressed at all (`chwi`).
    Drop,
    /// Two neighbouring keys in the wrong order (`chwoi`).
    Transpose,
    /// A tone or shape key that was never meant (`chowis`).
    ExtraModifier,
}

impl Fault {
    pub(in crate::dev) const ALL: [Fault; 4] = [
        Fault::Duplicate,
        Fault::Drop,
        Fault::Transpose,
        Fault::ExtraModifier,
    ];

    pub(in crate::dev) fn name(self) -> &'static str {
        match self {
            Fault::Duplicate => "duplicate",
            Fault::Drop => "drop",
            Fault::Transpose => "transpose",
            Fault::ExtraModifier => "extra-modifier",
        }
    }
}

/// How often a word gets a fault, and whether every fault is of one kind.
#[derive(Clone, Copy, PartialEq, Debug)]
pub(in crate::dev) struct Rates {
    /// The share of words that get one fault, in `0.0..=1.0`.
    pub(in crate::dev) overall: f32,
    /// Only this kind, or every kind equally likely.
    pub(in crate::dev) only: Option<Fault>,
}

/// The tone and shape keys of `method`, read off `key_role` rather than listed by
/// hand, so an extra modifier is always one the method would actually take.
pub(super) fn modifier_keys(method: InputMethod) -> Vec<char> {
    ('0'..='9')
        .chain('a'..='z')
        .filter(|&key| matches!(key_role(method, key), KeyRole::Tone | KeyRole::Shape))
        .collect()
}

/// `keys` with at most one fault in it, and which one, drawing only from `rng`.
pub(super) fn inject(
    keys: &str,
    method: InputMethod,
    rates: &Rates,
    rng: &mut Rng,
) -> (String, Option<Fault>) {
    let chars: Vec<char> = keys.chars().collect();
    if rates.overall <= 0.0 || chars.is_empty() || rng.uniform() > rates.overall {
        return (keys.to_owned(), None);
    }
    let fault = rates
        .only
        .unwrap_or_else(|| Fault::ALL[rng.below(Fault::ALL.len())]);
    match apply(fault, &chars, method, rng) {
        Some(faulty) => (faulty.into_iter().collect(), Some(fault)),
        None => (keys.to_owned(), None),
    }
}

/// `fault` done to `chars`, or `None` where the word is too short to take it.
fn apply(fault: Fault, chars: &[char], method: InputMethod, rng: &mut Rng) -> Option<Vec<char>> {
    let mut out = chars.to_vec();
    match fault {
        Fault::Duplicate => {
            let at = rng.below(chars.len());
            out.insert(at, chars[at]);
        }
        Fault::Drop => {
            if chars.len() < 2 {
                return None;
            }
            out.remove(rng.below(chars.len()));
        }
        Fault::Transpose => {
            // Swapping two equal keys changes nothing, so only a differing pair counts.
            let pairs: Vec<usize> = (0..chars.len().saturating_sub(1))
                .filter(|&i| chars[i] != chars[i + 1])
                .collect();
            if pairs.is_empty() {
                return None;
            }
            let at = pairs[rng.below(pairs.len())];
            out.swap(at, at + 1);
        }
        Fault::ExtraModifier => {
            let modifiers = modifier_keys(method);
            let key = modifiers[rng.below(modifiers.len())];
            // Never first: a modifier before any letter is a different kind of
            // nonsense, and the methods simply pass it through.
            out.insert(1 + rng.below(chars.len()), key);
        }
    }
    Some(out)
}
