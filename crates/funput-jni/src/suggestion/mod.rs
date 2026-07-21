//! Personal-suggestion JNI surface, grouped by concern: ID-based session
//! ownership (`registry`) and the exported `PersonalSuggestionNative` entry
//! points for lifecycle, learn/query, and store maintenance.

mod lifecycle;
mod query;
mod registry;
mod store;
