use funput_core::is_complete_syllable;

use super::SuggestionConfig;

/// How many sightings a token needs before the bar may offer it.
///
/// A complete Vietnamese syllable is something the language can spell, so it keeps the
/// ordinary threshold. Everything else waits longer: `đánb` is a slip, and a slip is
/// usually made once or twice, while `Zalo` or `hello` is a word somebody keeps coming
/// back to. Raising the bar rather than refusing outright is deliberate — the store is
/// a record of what its owner types, not of what a rule approves, and no rule knows
/// every name, brand or foreign word they are entitled to reuse.
///
/// The English dictionary deliberately stays out of this. It would recognise `hello`,
/// but reading a memory-mapped file on the learn path is exactly what
/// `english-lexicon-suggestion.md` forbids, and the tier already lets the word through
/// on its own merit.
pub(crate) fn promotion_threshold(token: &str, config: &SuggestionConfig) -> u32 {
    if is_complete_syllable(token) {
        config.promotion_uses
    } else {
        config.unrecognized_promotion_uses
    }
}
