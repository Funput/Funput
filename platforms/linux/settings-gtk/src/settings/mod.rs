mod model;
mod store;
mod types;

pub use funput_config::{ExtraOnsetLetters, OnsetLetter};
pub use model::*;
pub use types::*;

#[cfg(test)]
mod tests;
