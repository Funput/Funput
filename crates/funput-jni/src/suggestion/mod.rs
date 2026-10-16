//! Personal-suggestion JNI surface, grouped by concern: ID-based session
//! ownership (`registry`) and the exported `PersonalSuggestionNative` entry
//! points for lifecycle, learn/query, store maintenance, and the handle-free
//! shipped syllable bands (`syllables`).

mod lifecycle;
mod query;
mod registry;
mod store;
mod syllables;
