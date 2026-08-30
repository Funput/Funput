//! Personal-suggestion C ABI surface, grouped by concern: the opaque engine
//! handle (`engine`), learn/query calls (`query`), store maintenance (`store`),
//! and the POD result/stats types (`types`). Everything is re-exported flat from
//! the crate root so the C header sees a single `funput_suggestion_*` family.

mod engine;
mod query;
mod store;
mod types;

pub use engine::{
    FunputSuggestionEngine, funput_suggestion_engine_free, funput_suggestion_engine_new_in_memory,
    funput_suggestion_engine_open,
};
pub use query::{
    funput_suggestion_learn, funput_suggestion_learn_after, funput_suggestion_query,
    funput_suggestion_query_with,
};
pub use store::{
    funput_suggestion_compact, funput_suggestion_flush, funput_suggestion_reset,
    funput_suggestion_stats,
};
pub use types::{
    FunputSuggestionCandidate, FunputSuggestionResult, FunputSuggestionStats, SUGGESTION_CAP,
    SUGGESTION_CHARS_CAP,
};
