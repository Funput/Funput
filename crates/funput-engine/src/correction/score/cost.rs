//! What each kind of edit costs, in nats.
//!
//! The ladder is the model of *how* fast typing goes wrong, on the scale the touch
//! model already set: a substitution costs `λ` plus the evidence the finger gives
//! up, which for a real slip comes to about 1.5–2.5 nats. A key bouncing twice is
//! the commonest slip and the only edit priced below that; a stray modifier and two
//! fingers out of order sit above it; a key that never registered or was never
//! meant sits highest. Calibrated with `funput dev typos` on Viet74K (design doc
//! §12, Đợt 4): priced any lower, the structural readings — many of them real words
//! a cheap edit from a slip — land inside the margin of the slip's own repair and
//! turn it from "apply" into "ambiguous". Tiers in `generate` are built from these
//! sums, so changing one means checking the tier minimums it feeds.

/// A key pressed twice: `chowii` → `chơi`.
pub(crate) const DUP_DELETE: f32 = 1.5;
/// A tone, shape or stroke key that landed where none belongs: `chsowi` → `chơi`.
/// Priced like a transposition: paired with a substitution, dropping a tone key
/// reaches the toneless twin of nearly every toned word, and that twin is a real
/// word the slip's repair would then have to beat.
pub(crate) const MODIFIER_DELETE: f32 = 3.0;
/// Two neighbouring keys in the wrong order: `cohwi` → `chơi`.
pub(crate) const TRANSPOSE: f32 = 3.0;
/// Flat cost of substituting one key, in nats, before the touch evidence it trades.
pub(crate) const LAMBDA: f32 = 1.2;
/// Substituting a neighbour on the static layout, where no touch says how near the
/// finger came: the same `λ` with no evidence either way.
pub(crate) const SUBSTITUTE_STATIC: f32 = 1.2;
/// Any other key that was not meant.
pub(crate) const DELETE: f32 = 3.5;
/// A key that never registered: `chwi` → `chơi`. The dearest, and the only edit
/// that has to try every letter.
pub(crate) const INSERT: f32 = 3.5;
