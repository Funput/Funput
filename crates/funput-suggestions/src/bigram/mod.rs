//! Which words follow which, learned from the person typing.
//!
//! - `follower` — the edge itself, and how many of them a word keeps.
//! - `slots` — which edges survive when there are more than there is room for.
//! - `learning` — the engine's write path, [`SuggestionEngine::learn_after`].
//!
//! Nothing reads these edges yet; querying arrives with `suggest_with`.

pub(crate) mod follower;
mod learning;
mod slots;
