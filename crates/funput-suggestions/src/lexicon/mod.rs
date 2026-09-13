//! The English lexicon: a read-only word list shipped beside the personal store,
//! never inside it.
//!
//! - [`format`] — the `en.lex` layout and the rules every file must follow.
//! - `encode` — `en.tsv` to `en.lex`, for the build only.
//!
//! See `docs/features/english-lexicon-suggestion.md`.

#[cfg(any(test, feature = "lexicon-build"))]
pub(crate) mod encode;
pub(crate) mod format;
