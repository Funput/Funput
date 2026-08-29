/// The longest token the engine will look at, and so the size of any fixed
/// buffer that has to hold one. `sanitize` clamps to it, which is what lets a
/// caller-supplied word be normalized onto the stack.
pub(crate) const MAX_TOKEN_SCALARS: usize = 32;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SuggestionConfig {
    pub max_words: usize,
    pub max_token_scalars: usize,
    pub promotion_uses: u32,
    /// How often a pair must have been seen before the context may reorder the
    /// prefix results. One is deliberate: the prefix has already filtered the
    /// field, so the cost of being wrong is the order of three slots.
    pub context_rerank_uses: u16,
}

impl Default for SuggestionConfig {
    fn default() -> Self {
        Self {
            max_words: 5_000,
            max_token_scalars: 32,
            promotion_uses: 2,
            context_rerank_uses: 1,
        }
    }
}

pub(crate) fn sanitize(config: SuggestionConfig) -> SuggestionConfig {
    SuggestionConfig {
        max_words: config.max_words.max(1),
        max_token_scalars: config.max_token_scalars.clamp(1, MAX_TOKEN_SCALARS),
        promotion_uses: config.promotion_uses.max(1),
        context_rerank_uses: config.context_rerank_uses.max(1),
    }
}
