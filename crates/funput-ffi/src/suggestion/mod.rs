//! Personal-suggestion C ABI surface, grouped by concern: the opaque engine
//! handle and the lexicon attached to it (`engine`), learn/query calls
//! (`query`), the shipped Vietnamese data (`seed`), store maintenance (`store`),
//! the POD result/stats types (`types`), and the personal dictionary
//! (`dictionary/`). Everything is re-exported flat from the crate root so the C
//! header sees a single `funput_suggestion_*` family.

pub(crate) mod dictionary;
mod engine;
mod query;
mod seed;
mod store;
mod types;

pub use engine::{
    FunputSuggestionEngine, funput_suggestion_attach_lexicon, funput_suggestion_engine_free,
    funput_suggestion_engine_new_in_memory, funput_suggestion_engine_open,
};
pub use query::{
    funput_suggestion_frequency, funput_suggestion_is_known_word, funput_suggestion_learn,
    funput_suggestion_learn_after, funput_suggestion_query, funput_suggestion_query_in,
    funput_suggestion_query_with, funput_suggestion_syllable_tier,
    funput_suggestion_syllable_weight,
};
pub use seed::{
    FUNPUT_SEED_LANGUAGE_OFF, FUNPUT_SEED_LANGUAGE_VIETNAMESE, funput_suggestion_set_seed_language,
};
pub use store::{
    funput_suggestion_compact, funput_suggestion_flush, funput_suggestion_reset,
    funput_suggestion_stats,
};
pub use types::{
    FunputSuggestionCandidate, FunputSuggestionResult, FunputSuggestionStats, SUGGESTION_CAP,
    SUGGESTION_CHARS_CAP,
};
