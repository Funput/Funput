//! Who gets into the store and who makes room.
//!
//! - `admission` — how often a word has to be seen before learning offers it.
//! - `capacity` — which word makes room when the store is full, and trimming a
//!   store loaded under a smaller `max_words`.

pub(crate) mod admission;
mod capacity;
