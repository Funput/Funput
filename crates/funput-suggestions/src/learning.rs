use crate::engine::{PENDING_LIMIT, SuggestionEngine};
use crate::types::{LearnOutcome, WordRecord};

impl SuggestionEngine {
    pub fn learn(&mut self, token: &str) -> LearnOutcome {
        self.learn_inner(token, true)
    }

    pub(crate) fn learn_inner(&mut self, token: &str, record_pending: bool) -> LearnOutcome {
        let normalized = crate::normalize::exact(token);
        let length = normalized.chars().count();
        if length == 0
            || length > self.config.max_token_scalars
            || normalized.chars().any(char::is_whitespace)
        {
            return LearnOutcome::Ignored;
        }

        self.sequence = self.sequence.saturating_add(1);
        let (word_id, previous_uses, rebuild) = self.upsert_word(normalized.clone());
        if record_pending {
            if self.pending.len() < PENDING_LIMIT {
                self.pending.push(normalized);
            } else {
                self.pending_overflow = true;
            }
        }
        if rebuild {
            self.rebuild_tries();
        } else if self.words[word_id as usize].uses >= self.config.promotion_uses {
            self.index_word(word_id);
        }

        let uses = self.words[word_id as usize].uses;
        if previous_uses == 0 {
            LearnOutcome::Recorded
        } else if previous_uses < self.config.promotion_uses && uses >= self.config.promotion_uses {
            LearnOutcome::Promoted
        } else {
            LearnOutcome::Updated
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
        };
        if self.words.len() < self.config.max_words {
            let index = self.words.len() as u32;
            self.words.push(record);
            return (index, 0, false);
        }
        let index = self.lowest_ranked_index().unwrap_or(0);
        let rebuild = self.words[index].uses >= self.config.promotion_uses;
        self.words[index] = record;
        (index as u32, 0, rebuild)
    }

    pub(crate) fn index_word(&mut self, word_id: u32) {
        let word = &self.words[word_id as usize].text;
        self.exact
            .insert(crate::normalize::exact_chars(word), word_id, &self.words);
        let folded = crate::normalize::folded(word);
        if folded != *word {
            self.folded.insert(folded.chars(), word_id, &self.words);
        }
    }

    pub(crate) fn rebuild_tries(&mut self) {
        self.exact.clear();
        self.folded.clear();
        for index in 0..self.words.len() {
            if self.words[index].uses >= self.config.promotion_uses {
                self.index_word(index as u32);
            }
        }
    }
}
