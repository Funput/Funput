/// The longest token the engine will look at, and so the size of any fixed
/// buffer that has to hold one. `sanitize` clamps to it, which is what lets a
/// caller-supplied word be normalized onto the stack.
pub(crate) const MAX_TOKEN_SCALARS: usize = 32;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SuggestionConfig {
    pub max_words: usize,
    pub max_token_scalars: usize,
    pub promotion_uses: u32,
    /// The same, for a token that is not a Vietnamese syllable — a mistype, a name, a
    /// foreign word. See [`super::admission`].
    pub unrecognized_promotion_uses: u32,
    /// How often a pair must have been seen before the context may reorder the
    /// prefix results. One is deliberate: the prefix has already filtered the
    /// field, so the cost of being wrong is the order of three slots.
    pub context_rerank_uses: u16,
    /// How often a pair must have been seen before it may be offered with no
    /// prefix at all. Higher than the rerank threshold, because here being wrong
    /// costs a slot rather than an order.
    pub context_predict_uses: u16,
    /// What share of a context word's sightings its strongest follower has to
    /// account for before the bar says anything. This is the whole of the
    /// feature's judgement: "cảm" is nearly always followed by "ơn" and clears it
    /// at once, while "của" can be followed by anything and never will.
    pub context_dominance_percent: u8,
    /// How often the user may type a word as a context before the shipped pair
    /// list stops answering for it. Past this, a Predict that says nothing is the
    /// user's own data speaking, and the list stays quiet rather than argue.
    pub seed_pairs_until_seen: u16,
    /// How many promoted words carrying Vietnamese marks make the store a
    /// Vietnamese one. From then on, a prefix whose personal answer holds such a
    /// word is Vietnamese typing, and the English lexicon stays out of its empty
    /// slots. Zero yields from the first marked word.
    pub lexicon_yield_after_words: u32,
    /// How many words may be added by hand. Always fewer than `max_words`, so a
    /// full store keeps a learned word to evict.
    pub max_added_words: usize,
    /// How many words may be blocked.
    pub max_blocked_words: usize,
}

impl Default for SuggestionConfig {
    fn default() -> Self {
        Self {
            max_words: 5_000,
            max_token_scalars: 32,
            promotion_uses: 2,
            unrecognized_promotion_uses: 4,
            context_rerank_uses: 1,
            context_predict_uses: 2,
            context_dominance_percent: 40,
            seed_pairs_until_seen: 3,
            lexicon_yield_after_words: 200,
            max_added_words: 1_000,
            max_blocked_words: 1_000,
        }
    }
}

pub(crate) fn sanitize(config: SuggestionConfig) -> SuggestionConfig {
    let max_words = config.max_words.max(1);
    SuggestionConfig {
        max_words,
        max_token_scalars: config.max_token_scalars.clamp(1, MAX_TOKEN_SCALARS),
        promotion_uses: config.promotion_uses.max(1),
        // Never the easier of the two, whatever a caller passes.
        unrecognized_promotion_uses: config
            .unrecognized_promotion_uses
            .max(config.promotion_uses.max(1)),
        context_rerank_uses: config.context_rerank_uses.max(1),
        context_predict_uses: config.context_predict_uses.max(1),
        context_dominance_percent: config.context_dominance_percent.min(100),
        seed_pairs_until_seen: config.seed_pairs_until_seen.max(1),
        lexicon_yield_after_words: config.lexicon_yield_after_words,
        max_added_words: config.max_added_words.min(max_words - 1),
        max_blocked_words: config.max_blocked_words,
    }
}
