//! The personal dictionary over the C ABI.
//!
//! - `edit` — changes through the handle that owns the store (the keyboard).
//! - `view` — the app's side: a read-only open, and posting edits without a handle.
//! - `list` — listings as an owned, opaque handle read entry by entry.
//! - `types` — the entry struct and the `FUNPUT_DICTIONARY_*` codes.

mod edit;
mod list;
mod types;
mod view;

pub use edit::{funput_suggestion_apply_edit, funput_suggestion_apply_pending_edits};
pub use list::{
    FunputDictionaryList, funput_suggestion_blocked_list, funput_suggestion_dictionary_list,
    funput_suggestion_dictionary_list_entry, funput_suggestion_dictionary_list_free,
    funput_suggestion_dictionary_list_len,
};
pub use types::{
    FUNPUT_DICTIONARY_APPLIED, FUNPUT_DICTIONARY_EDIT_ADD, FUNPUT_DICTIONARY_EDIT_BLOCK,
    FUNPUT_DICTIONARY_EDIT_FORGET, FUNPUT_DICTIONARY_EDIT_UNBLOCK, FUNPUT_DICTIONARY_FILTER_ADDED,
    FUNPUT_DICTIONARY_FILTER_ALL, FUNPUT_DICTIONARY_FILTER_LEARNED, FUNPUT_DICTIONARY_INVALID,
    FUNPUT_DICTIONARY_LIMIT_REACHED, FUNPUT_DICTIONARY_ORIGIN_ADDED,
    FUNPUT_DICTIONARY_ORIGIN_BLOCKED, FUNPUT_DICTIONARY_ORIGIN_LEARNED,
    FUNPUT_DICTIONARY_SORT_ALPHABETICAL, FUNPUT_DICTIONARY_SORT_MOST_USED,
    FUNPUT_DICTIONARY_SORT_RECENT, FUNPUT_DICTIONARY_UNCHANGED, FunputDictionaryEntry,
};
pub use view::{funput_suggestion_engine_open_snapshot, funput_suggestion_post_edit};
