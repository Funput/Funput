//! Confirm a circumflex the composer added on its own instead of reverting it.
//!
//! Placing a tone on `ie` + coda promotes the rhyme to `iê`, so `viecj` is already
//! `việc`. The UniKey habit of typing the tone first and the mũ last (`viecje`) then
//! hands the core an `ê` it never saw typed, and the mũ key reverts it: `viẹce`.
//! Here that key is read as the user confirming the mũ: it is swallowed, the word
//! stays. Design: `docs/features/implied-circumflex-confirm.md`.
//!
//! The rule ([`confirmed`]) — every condition must hold:
//! 1. The core reverted the mũ key. On that key a revert can only be a circumflex
//!    revert, so nothing else needs inspecting.
//! 2. The raw keys before it are ASCII and hold fewer mũ keys than a typed `ê`
//!    needs ([`confirms_implied_circumflex`]). Non-ASCII keys were rewritten from
//!    the screen by a revert, Backspace or `adopt` and carry no record of who typed
//!    the mũ, so such a word keeps the old revert (the line `doubled::on_revert`
//!    draws too).
//! 3. The word on screen is a complete syllable with a standard onset
//!    ([`standard_shape`]): a confirm means the user is finishing the Vietnamese
//!    word whose mũ was promoted, and English with `ie`, a consonant and then `e`
//!    (`directed` shows `diểct`, `quiesce` shows `quiếc`, `Irene` shows `Iển`,
//!    `briefness` shows `briền`) is not that word.

use funput_core::{ExtraOnsets, InputMethod};

use crate::model::Session;

/// The onsets Vietnamese spells before `iê`, lowercase. The empty onset is not here
/// (`iê` alone is written `yê`), nor is `qu` (`quyê`), nor the ethnic and place-name
/// clusters the syllable rules admit (`br`, `pr`, `dr`): all three are shapes only an
/// English word reaches.
const ONSETS: [&str; 25] = [
    "b", "c", "ch", "d", "đ", "g", "gh", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh", "p",
    "ph", "r", "s", "t", "th", "tr", "v", "x",
];

/// Whether `key`, which the core has just reverted, confirms a circumflex the
/// composer added. The cheap key-count predicate runs first; the syllable check,
/// which builds a rhyme string, only once it has passed.
pub(super) fn confirmed(session: &Session, key: char) -> bool {
    let rules = session.config.syllable_rules;
    session
        .keys
        .strip_suffix(key)
        .is_some_and(|earlier| confirms_implied_circumflex(session.config.method, earlier, key))
        && standard_shape(&session.buffer, rules.extra_onsets)
        && rules.is_complete_syllable(&session.buffer)
}

/// Whether the raw keys `earlier`, typed before the mũ key `key`, hold fewer mũ keys
/// than a typed `ê` needs: two `e` in the Telex family (`ee`), one `6` in VNI. Fewer
/// means the `ê` on screen was never typed.
fn confirms_implied_circumflex(method: InputMethod, earlier: &str, key: char) -> bool {
    let (mu_key, typed_needs) = match method {
        InputMethod::Telex | InputMethod::TelexAdvanced => ('e', 2),
        InputMethod::Vni => ('6', 1),
        // `InputMethod` is `#[non_exhaustive]`: a method this crate does not know
        // has no known mũ key, so its reverts stay reverts.
        _ => return false,
    };
    earlier.is_ascii()
        && key.eq_ignore_ascii_case(&mu_key)
        && earlier
            .chars()
            .filter(|typed| typed.eq_ignore_ascii_case(&mu_key))
            .count()
            < typed_needs
}

/// Whether the letters before the first vowel of `buffer` are one of [`ONSETS`], or
/// one of the extra onsets (`z`, `f`, `w`, `j`) the user switched on.
fn standard_shape(buffer: &str, extra: ExtraOnsets) -> bool {
    let onset = &buffer[..buffer.find(is_vowel).unwrap_or(buffer.len())];
    let extra_onset = |letter: char, flag: ExtraOnsets| {
        extra.contains(flag)
            && onset.len() == 1
            && onset.starts_with(|c: char| c.eq_ignore_ascii_case(&letter))
    };
    ONSETS
        .iter()
        .any(|standard| onset.eq_ignore_ascii_case(standard))
        || onset == "Đ" // `eq_ignore_ascii_case` folds ASCII only, never `Đ` to `đ`
        || extra_onset('z', ExtraOnsets::Z)
        || extra_onset('f', ExtraOnsets::F)
        || extra_onset('w', ExtraOnsets::W)
        || extra_onset('j', ExtraOnsets::J)
}

/// A vowel of the composed buffer: a plain Latin vowel, or any shaped or toned
/// letter, which in a composition is always a vowel except `đ`.
fn is_vowel(c: char) -> bool {
    matches!(c.to_ascii_lowercase(), 'a' | 'e' | 'i' | 'o' | 'u' | 'y')
        || (!c.is_ascii() && !matches!(c, 'đ' | 'Đ'))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn session(buffer: &str, keys: &str) -> Session {
        let mut session = Session::new();
        session.buffer.push_str(buffer);
        session.keys.push_str(keys);
        session
    }

    #[test]
    fn telex_confirms_after_a_single_e() {
        assert!(confirms_implied_circumflex(
            InputMethod::Telex,
            "viecj",
            'e'
        ));
        assert!(confirms_implied_circumflex(
            InputMethod::Telex,
            "VIECJ",
            'E'
        ));
        assert!(confirms_implied_circumflex(
            InputMethod::Telex,
            "Viecj",
            'e'
        ));
        assert!(confirms_implied_circumflex(
            InputMethod::TelexAdvanced,
            "tiengs",
            'e'
        ));
    }

    #[test]
    fn a_circumflex_the_user_typed_still_reverts() {
        assert!(!confirms_implied_circumflex(
            InputMethod::Telex,
            "vieecj",
            'e'
        ));
        assert!(!confirms_implied_circumflex(
            InputMethod::Telex,
            "viecje",
            'e'
        ));
        assert!(!confirms_implied_circumflex(
            InputMethod::TelexAdvanced,
            "vieecj",
            'e'
        ));
        assert!(!confirms_implied_circumflex(
            InputMethod::Vni,
            "viec56",
            '6'
        ));
    }

    #[test]
    fn only_the_circumflex_key_confirms() {
        assert!(!confirms_implied_circumflex(
            InputMethod::Telex,
            "viecj",
            'j'
        ));
        assert!(!confirms_implied_circumflex(
            InputMethod::Telex,
            "muonj",
            'o'
        ));
        assert!(!confirms_implied_circumflex(InputMethod::Vni, "viec5", 'e'));
        assert!(!confirms_implied_circumflex(InputMethod::Vni, "muon5", '7'));
    }

    #[test]
    fn vni_confirms_on_the_first_six() {
        assert!(confirms_implied_circumflex(InputMethod::Vni, "viec5", '6'));
        assert!(confirms_implied_circumflex(InputMethod::Vni, "TIENG1", '6'));
    }

    #[test]
    fn keys_rewritten_from_the_screen_never_confirm() {
        assert!(!confirms_implied_circumflex(
            InputMethod::Telex,
            "việc",
            'e'
        ));
        assert!(!confirms_implied_circumflex(InputMethod::Telex, "viê", 'e'));
        assert!(!confirms_implied_circumflex(InputMethod::Vni, "việc", '6'));
    }

    #[test]
    fn only_a_complete_syllable_on_screen_confirms() {
        assert!(confirmed(&session("việc", "viecje"), 'e'));
        assert!(confirmed(&session("nghiềng", "nghiengfe"), 'e'));
        assert!(confirmed(&session("Điện", "Dienje"), 'e'));
        assert!(!confirmed(&session("diểct", "directe"), 'e'));
        assert!(!confirmed(&session("viêc", "viecjze"), 'e'));
    }

    #[test]
    fn only_a_standard_onset_confirms() {
        assert!(!confirmed(&session("iển", "irene"), 'e'));
        assert!(!confirmed(&session("Iển", "Irene"), 'e'));
        assert!(!confirmed(&session("quiếc", "quiesce"), 'e'));
        assert!(!confirmed(&session("briền", "briefne"), 'e'));
        assert!(!confirmed(&session("priến", "priestee"), 'e'));
        assert!(!confirmed(&session("fiểm", "fireme"), 'e'));
    }

    #[test]
    fn an_extra_onset_the_user_enabled_confirms() {
        let mut session = session("fiểm", "fireme");
        session.config.syllable_rules = session
            .config
            .syllable_rules
            .with_extra_onsets(ExtraOnsets::F);
        assert!(confirmed(&session, 'e'));
    }
}
