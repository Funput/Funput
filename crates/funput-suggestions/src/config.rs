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
        max_token_scalars: config.max_token_scalars.clamp(1, 32),
        promotion_uses: config.promotion_uses.max(1),
    }
}
