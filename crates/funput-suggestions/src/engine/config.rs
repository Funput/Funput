/// The longest token the engine will look at, and so the size of any fixed
/// buffer that has to hold one. `sanitize` clamps to it, which is what lets a
/// caller-supplied word be normalized onto the stack.
pub(crate) const MAX_TOKEN_SCALARS: usize = 32;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SuggestionConfig {
    pub max_words: usize,
    pub max_token_scalars: usize,
    pub promotion_uses: u32,
}

impl Default for SuggestionConfig {
    fn default() -> Self {
        Self {
            max_words: 5_000,
            max_token_scalars: 32,
            promotion_uses: 2,
        }
    }
}

pub(crate) fn sanitize(config: SuggestionConfig) -> SuggestionConfig {
    SuggestionConfig {
        max_words: config.max_words.max(1),
        max_token_scalars: config.max_token_scalars.clamp(1, MAX_TOKEN_SCALARS),
        promotion_uses: config.promotion_uses.max(1),
    }
}
