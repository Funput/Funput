use super::admission::promotion_threshold;
use super::{PENDING_LIMIT, REBUILD_AFTER_EVICTIONS, SuggestionEngine};
use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::index::{NONE, normalize};
use crate::persistence::JournalEntry;
use crate::types::{LearnOutcome, WordRecord};

impl SuggestionEngine {
    pub fn learn(&mut self, token: &str) -> LearnOutcome {
        self.learn_after(None, token)
    }

    /// Learn one token, returning the outcome and the slot it landed in — `NONE`
    /// when it was ignored. Appending it to the journal is `learn_after_inner`'s
    /// job, because only that knows whether it followed what came before.
    pub(crate) fn learn_inner(&mut self, token: &str) -> (LearnOutcome, u32) {
        let normalized = normalize::exact(token);
        let length = normalized.chars().count();
        if length == 0
            || length > self.config.max_token_scalars
            || normalized.chars().any(char::is_whitespace)
        {
            return (LearnOutcome::Ignored, NONE);
        }

        self.sequence = self.sequence.saturating_add(1);
        let (word_id, previous_uses, evicted_indexed) = self.upsert_word(normalized);
        if evicted_indexed {
            // The evicted word's entries are stale, not wrong: its slot's
            // generation moved on, so they already read as dead. Sweeping them is
            // bookkeeping, and bookkeeping does not belong between two keystrokes.
            self.evictions_since_rebuild = self.evictions_since_rebuild.saturating_add(1);
        }
        let uses = self.words[word_id as usize].uses;
        let threshold = promotion_threshold(&self.words[word_id as usize].text, &self.config);
        if uses >= threshold {
            let marked = self.index_word(word_id);
            // Counted before any rebuild below, which recounts from scratch and
            // would otherwise see this word twice.
            if marked && previous_uses < threshold {
                self.lexicon.vietnamese_words = self.lexicon.vietnamese_words.saturating_add(1);
            }
        }
        if self.evictions_since_rebuild >= REBUILD_AFTER_EVICTIONS {
            self.rebuild_tries();
        }

        let outcome = if previous_uses == 0 {
            LearnOutcome::Recorded
        } else if previous_uses < threshold && uses >= threshold {
            LearnOutcome::Promoted
        } else {
            LearnOutcome::Updated
        };
        (outcome, word_id)
    }

    /// Overflow is not a gap in the journal: `flush` turns it into a compact,
    /// which replaces the journal outright rather than appending a chain with a
    /// hole nothing marked.
    pub(crate) fn push_pending(&mut self, entry: JournalEntry) {
        if self.pending.len() < PENDING_LIMIT {
            self.pending.push(entry);
        } else {
            self.pending_overflow = true;
        }
    }

    fn upsert_word(&mut self, normalized: String) -> (u32, u32, bool) {
        if let Some(index) = self.words.iter().position(|word| word.text == normalized) {
            let word = &mut self.words[index];
            let previous = word.uses;
            word.uses = word.uses.saturating_add(1);
            word.last_used = self.sequence;
            return (index as u32, previous, false);
        }
        let record = WordRecord {
            text: normalized,
            uses: 1,
            last_used: self.sequence,
            generation: 0,
            context_seen: 0,
            followers: [Follower::EMPTY; FOLLOWER_SLOTS],
        };
        if self.words.len() < self.config.max_words {
            let index = self.words.len() as u32;
            self.words.push(record);
            return (index, 0, false);
        }
        let index = self.lowest_ranked_index().unwrap_or(0);
        let rebuild =
            self.words[index].uses >= promotion_threshold(&self.words[index].text, &self.config);
        if rebuild && normalize::is_marked(&self.words[index].text) {
            self.lexicon.vietnamese_words = self.lexicon.vietnamese_words.saturating_sub(1);
        }
        // Wrapping is unreachable in practice: it would take 65,536 reuses of one
        // slot with no rebuild in between, and a rebuild clears every stale entry.
        let generation = self.words[index].generation.wrapping_add(1);
        self.words[index] = record;
        self.words[index].generation = generation;
        (index as u32, 0, rebuild)
    }

    /// Returns whether the word is marked, which is exactly whether it went into
    /// the folded trie as well.
    pub(crate) fn index_word(&mut self, word_id: u32) -> bool {
        let word = &self.words[word_id as usize].text;
        self.exact
            .insert(normalize::exact_chars(word), word_id, &self.words);
        let folded = normalize::folded(word);
        let marked = folded != *word;
        if marked {
            self.folded.insert(folded.chars(), word_id, &self.words);
        }
        marked
    }

    /// Also recounts the promoted marked words, which puts right any drift in
    /// the running count whenever the tries are swept: on open, and on a flush
    /// after evictions.
    pub(crate) fn rebuild_tries(&mut self) {
        self.rebuilds = self.rebuilds.saturating_add(1);
        self.evictions_since_rebuild = 0;
        self.exact.clear();
        self.folded.clear();
        let mut marked = 0u32;
        for index in 0..self.words.len() {
            let threshold = promotion_threshold(&self.words[index].text, &self.config);
            if self.words[index].uses >= threshold && self.index_word(index as u32) {
                marked = marked.saturating_add(1);
            }
        }
        self.lexicon.vietnamese_words = marked;
    }
}
