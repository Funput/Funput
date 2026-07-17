//! `uo`, `uơ`, and `ưo` horn-compound normalization.

mod apply;
mod continuation;
mod pair;
mod revert;

pub(crate) use apply::{apply_uo_compound, apply_uo_compound_in_place};
pub(crate) use continuation::{complete_uo_horn_for_continuation, normalize_horned_uo_open};
pub(crate) use pair::{ends_with_open_uo_horn, uo_pair_in_vowel_cluster};
pub(crate) use revert::try_revert_uo_compound;

#[cfg(test)]
mod tests;
