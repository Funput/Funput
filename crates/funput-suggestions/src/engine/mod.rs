//! The [`SuggestionEngine`] facade — its state, lifecycle (`in_memory` / `open`),
//! and capacity policy — plus the behaviours split by concern into [`learning`]
//! (learn), [`query`] (suggest/stats), and [`durability`] (flush/compact/reset).

mod config;
mod durability;
mod learning;
mod query;

pub use config::SuggestionConfig;

use std::io;
use std::path::Path;

use config::sanitize;

use crate::index::ArenaTrie;
use crate::persistence::Store;
use crate::types::WordRecord;

pub(crate) const PENDING_LIMIT: usize = 256;
pub(crate) const JOURNAL_COMPACT_BYTES: u64 = 64 * 1024;

pub struct SuggestionEngine {
    pub(crate) config: SuggestionConfig,
    pub(crate) words: Vec<WordRecord>,
    pub(crate) exact: ArenaTrie,
    pub(crate) folded: ArenaTrie,
    pub(crate) sequence: u64,
    pub(crate) pending: Vec<String>,
    pub(crate) pending_overflow: bool,
    pub(crate) store: Option<Store>,
    pub(crate) last_snapshot_bytes: u64,
    pub(crate) journal_bytes: u64,
}

impl SuggestionEngine {
    pub fn in_memory(config: SuggestionConfig) -> Self {
        Self {
            config: sanitize(config),
            words: Vec::new(),
            exact: ArenaTrie::new(),
            folded: ArenaTrie::new(),
            sequence: 0,
            pending: Vec::with_capacity(PENDING_LIMIT),
            pending_overflow: false,
            store: None,
            last_snapshot_bytes: 0,
            journal_bytes: 0,
        }
    }

    pub fn open(path: impl AsRef<Path>, config: SuggestionConfig) -> io::Result<Self> {
        let config = sanitize(config);
        let (store, loaded) = Store::open(path.as_ref())?;
        let mut engine = Self::in_memory(config);
        engine.store = Some(store);
        engine.words = loaded.words;
        engine.sequence = loaded.sequence;
        engine.journal_bytes = loaded.journal_bytes;
        engine.enforce_capacity();
        engine.rebuild_tries();
        for token in loaded.journal {
            engine.learn_inner(&token, false);
        }
        engine.pending.clear();
        engine.pending_overflow = false;
        Ok(engine)
    }

    pub(crate) fn lowest_ranked_index(&self) -> Option<usize> {
        self.words
            .iter()
            .enumerate()
            .min_by(|(_, left), (_, right)| {
                left.uses
                    .cmp(&right.uses)
                    .then_with(|| left.last_used.cmp(&right.last_used))
                    .then_with(|| right.text.cmp(&left.text))
            })
            .map(|(index, _)| index)
    }

    fn enforce_capacity(&mut self) {
        while self.words.len() > self.config.max_words {
            let Some(index) = self.lowest_ranked_index() else {
                break;
            };
            self.words.swap_remove(index);
        }
    }
}
