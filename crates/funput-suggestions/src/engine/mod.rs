//! The [`SuggestionEngine`] facade — its state, lifecycle (`in_memory` / `open`),
//! and capacity policy — plus the behaviours split by concern into [`learning`]
//! (learn), [`query`] (suggest/stats), and [`durability`] (flush/compact/reset).
//! [`admission`] decides how often a word has to be seen before learning offers it.

pub(crate) mod admission;
mod config;
mod durability;
mod learning;
mod query;

pub(crate) use config::MAX_TOKEN_SCALARS;
pub use config::SuggestionConfig;

use std::io;
use std::path::Path;

use config::sanitize;

use crate::dictionary::BlockedWords;
use crate::dictionary::Origin;
use crate::index::ArenaTrie;
use crate::lexicon::LexiconSlot;
use crate::persistence::{JournalEntry, Loaded, Store};
use crate::seed::SeedSlot;
use crate::types::WordRecord;

pub(crate) const PENDING_LIMIT: usize = 256;

/// How many dead trie entries the learn path may leave behind before it stops
/// waiting for an idle moment and sweeps them itself.
///
/// `flush` normally sweeps long first — both shells call it every 32 learns and
/// on a 2 s timer — so this is the bound for a caller that never flushes, not the
/// common path. Without it the tries would grow with every distinct word ever
/// seen, which is the one shape of unbounded memory this change could introduce.
/// At roughly 1.4 ms per rebuild it amortises to about 22 µs per eviction,
/// against 1.4 ms when every eviction rebuilt on the spot.
pub(crate) const REBUILD_AFTER_EVICTIONS: u32 = 64;
pub(crate) const JOURNAL_COMPACT_BYTES: u64 = 64 * 1024;

pub struct SuggestionEngine {
    pub(crate) config: SuggestionConfig,
    pub(crate) words: Vec<WordRecord>,
    pub(crate) exact: ArenaTrie,
    pub(crate) folded: ArenaTrie,
    pub(crate) sequence: u64,
    pub(crate) pending: Vec<JournalEntry>,
    pub(crate) pending_overflow: bool,
    pub(crate) store: Option<Store>,
    pub(crate) last_snapshot_bytes: u64,
    pub(crate) journal_bytes: u64,
    /// How many times the tries have been rebuilt from scratch. Not part of
    /// `SuggestionStats` — that struct crosses the C ABI and the JNI boundary,
    /// and this exists so the crate's own tests can assert the learn path never
    /// triggers one.
    pub(crate) rebuilds: u64,
    /// Indexed words evicted since the last rebuild — that is, how many dead
    /// entries the tries are currently carrying.
    pub(crate) evictions_since_rebuild: u32,
    /// The slot and generation of the last token appended to the journal.
    ///
    /// Replay rebuilds pairs from adjacency, so the writer has to know whether
    /// the context it was handed really is what it wrote last. When it is not, a
    /// break goes in and replay loses an edge rather than inventing one.
    pub(crate) journalled_previous: Option<(u32, u16)>,
    /// The shipped English word list, beside the personal store, never in it.
    pub(crate) lexicon: LexiconSlot,
    /// The words the user blocked: never learned, never offered, English
    /// lexicon included.
    pub(crate) blocked: BlockedWords,
    /// The shipped Vietnamese data, and whether it may speak.
    pub(crate) seed: SeedSlot,
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
            rebuilds: 0,
            evictions_since_rebuild: 0,
            journalled_previous: None,
            lexicon: LexiconSlot::default(),
            blocked: BlockedWords::default(),
            seed: SeedSlot::default(),
        }
    }

    /// Opens the store at `path` for this process to write, and applies any edits
    /// the app has posted to it since it was last open.
    pub fn open(path: impl AsRef<Path>, config: SuggestionConfig) -> io::Result<Self> {
        let (store, loaded) = Store::open(path.as_ref())?;
        let mut engine = Self::from_loaded(config, loaded);
        engine.store = Some(store);
        // Best effort: an inbox that cannot be read now is read on the next drain,
        // and the store itself opened fine.
        let _ = engine.apply_pending_edits();
        Ok(engine)
    }

    /// An engine holding what was read off disk, with no store to write it back to.
    pub(crate) fn from_loaded(config: SuggestionConfig, loaded: Loaded) -> Self {
        let mut engine = Self::in_memory(config);
        engine.words = loaded.words;
        engine.blocked = BlockedWords::from_loaded(loaded.blocked);
        engine.sequence = loaded.sequence;
        engine.journal_bytes = loaded.journal_bytes;
        engine.enforce_capacity();
        engine.rebuild_tries();
        engine.replay(loaded.journal);
        engine
    }

    /// The learned word that should make room next. Words added by hand never do.
    pub(crate) fn lowest_ranked_index(&self) -> Option<usize> {
        self.words
            .iter()
            .enumerate()
            .filter(|(_, word)| word.origin == Origin::Learned)
            .min_by(|(_, left), (_, right)| {
                left.uses
                    .cmp(&right.uses)
                    .then_with(|| left.last_used.cmp(&right.last_used))
                    .then_with(|| right.text.cmp(&left.text))
            })
            .map(|(index, _)| index)
    }

    /// Only ever called while loading, which rebuilds the tries straight after:
    /// `remove_slot` leaves them stale.
    fn enforce_capacity(&mut self) {
        while self.words.len() > self.config.max_words {
            let Some(index) = self.lowest_ranked_index() else {
                break;
            };
            self.remove_slot(index);
        }
    }
}
