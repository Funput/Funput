//! Re-opening a committed word for editing (`Engine::adopt`).
//!
//! Android calls this when Backspace puts the caret back on a finished word, so the
//! next keystroke retones it instead of typing a literal letter.

use funput_core::{ExtraOnsets, InputMethod, SyllableRules};
use funput_engine::Engine;

fn engine(method: InputMethod) -> Engine {
    let mut engine = Engine::new();
    engine.set_method(method);
    engine
}

fn type_into(engine: &mut Engine, keys: &str) -> String {
    for key in keys.chars() {
        engine.process_char(key);
    }
    engine.buffer().to_owned()
}

/// The reason this exists: type `chào`, commit it with Space, delete the space, adopt
/// the word again, then a tone key edits it in place.
#[test]
fn adopted_word_accepts_a_new_tone() {
    let mut engine = engine(InputMethod::Telex);
    assert_eq!(type_into(&mut engine, "chaof"), "chào");
    engine.process_char(' '); // word boundary commits and clears the session
    assert_eq!(engine.buffer(), "");

    assert!(engine.adopt("chào"));
    assert_eq!(engine.buffer(), "chào");
    engine.process_char('s'); // sắc replaces huyền
    assert_eq!(engine.buffer(), "cháo");
}

#[test]
fn adopted_word_matches_typing_it_from_scratch() {
    let mut adopted = engine(InputMethod::Telex);
    assert!(adopted.adopt("chào"));
    adopted.process_char('s');

    let mut typed = engine(InputMethod::Telex);
    assert_eq!(type_into(&mut typed, "chaofs"), adopted.buffer());
}

#[test]
fn adopts_a_capitalized_syllable() {
    // Sentence-initial words must be re-editable too.
    let mut engine = engine(InputMethod::Telex);
    assert!(engine.adopt("Chào"));
    engine.process_char('s');
    assert_eq!(engine.buffer(), "Cháo");
}

#[test]
fn adopts_a_toneless_syllable_and_adds_a_tone() {
    let mut engine = engine(InputMethod::Vni);
    assert!(engine.adopt("chao"));
    engine.process_char('2'); // VNI huyền
    assert_eq!(engine.buffer(), "chào");
}

/// A stop coda (`p t c ch`) with no tone yet is not a *complete* syllable, but it is
/// exactly what a committed word looks like before its tone: `chuc` + Space + ⌫ + `s`
/// has to give `chúc`, not `chucs`.
#[test]
fn adopts_a_syllable_still_missing_its_stop_coda_tone() {
    for (word, key, expected) in [
        ("chuc", 's', "chúc"),
        ("tich", 's', "tích"),
        ("hoc", 'j', "học"),
    ] {
        let mut engine = engine(InputMethod::Telex);
        assert!(engine.adopt(word), "should adopt {word:?}");
        engine.process_char(key);
        assert_eq!(engine.buffer(), expected);
    }
}

/// The same story one step earlier: a word committed before its *vowel shape* — the
/// rhyme only exists in Vietnamese shaped (`ien` is `iên` without its circumflex), so
/// re-opening has to hand it back to the shape and stroke keys too.
#[test]
fn adopts_a_syllable_still_missing_its_vowel_shape() {
    for (word, key, expected) in [
        ("vien", 'e', "viên"),   // circumflex
        ("thuo", 'w', "thuơ"),   // horn
        ("dien", 'd', "đien"),   // stroke
        ("nuoc", 'w', "nươc"),   // shape while the tone is still missing too
        ("duong", 'w', "dương"), // ươ pair
        ("cuu", 'w', "cưu"),
    ] {
        let mut engine = engine(InputMethod::Telex);
        assert!(engine.adopt(word), "should adopt {word:?}");
        engine.process_char(key);
        assert_eq!(engine.buffer(), expected, "{word:?} + {key:?}");
    }
}

/// The invariant behind every case above: re-opening a word puts the engine in the
/// state it would have been in had the word never been committed. Whatever the next
/// key does to a live composition, it does to a re-opened one.
#[test]
fn a_reopened_word_edits_exactly_like_a_live_one() {
    for (word, key) in [
        ("vien", 'e'),
        ("thuo", 'w'),
        ("dien", 'd'),
        ("nuoc", 's'),
        ("chuc", 's'),
        ("tuoi", 'o'),
    ] {
        let mut adopted = engine(InputMethod::Telex);
        assert!(adopted.adopt(word), "should adopt {word:?}");
        adopted.process_char(key);

        let mut typed = engine(InputMethod::Telex);
        type_into(&mut typed, word);
        typed.process_char(key);

        assert_eq!(adopted.buffer(), typed.buffer(), "{word:?} + {key:?}");
    }
}

/// Anything that is not a Vietnamese syllable is refused, so English words and URLs
/// never silently become editable. `die` is the near miss: no Vietnamese rhyme is
/// spelled `ie`, however many diacritics you add.
#[test]
fn refuses_text_that_is_not_a_syllable() {
    let mut engine = engine(InputMethod::Telex);
    for text in ["", "text", "tẽt", "hello", "die", "funput.app", "chào bạn"] {
        assert!(!engine.adopt(text), "should not adopt {text:?}");
        assert_eq!(engine.buffer(), "", "buffer must stay empty after refusing");
    }
}

#[test]
fn refuses_while_disabled() {
    let mut engine = engine(InputMethod::Telex);
    engine.set_enabled(false);
    assert!(!engine.adopt("chào"));
    assert_eq!(engine.buffer(), "");
}

/// Adopting replaces whatever was composing, and leaves no raw form to flip back to
/// (the keystrokes that produced the word are gone).
#[test]
fn adopting_replaces_composition_and_leaves_nothing_to_flip() {
    let mut engine = engine(InputMethod::Telex);
    type_into(&mut engine, "meo");
    assert!(engine.adopt("chào"));
    assert_eq!(engine.buffer(), "chào");
    assert_eq!(engine.keys(), "chào");
    assert_eq!(engine.flip_composing().action, funput_engine::Action::None);
}

/// A word boundary after editing an adopted word commits the edited form, not the
/// original raw keys (English restore must not fire on valid Vietnamese).
#[test]
fn boundary_after_editing_keeps_the_vietnamese_form() {
    let mut engine = engine(InputMethod::Telex);
    assert!(engine.adopt("chào"));
    engine.process_char('s');
    let result = engine.process_char(' ');
    let committed = if result.action == funput_engine::Action::None {
        "cháo ".to_owned()
    } else {
        result.output
    };
    assert_eq!(committed, "cháo ");
}

/// A word only the widened spelling admits re-opens only while it is admitted.
#[test]
fn adopt_follows_the_syllable_rules() {
    let mut native = engine(InputMethod::Telex);
    assert!(!native.adopt("zô"));

    let mut widened = engine(InputMethod::Telex);
    widened.update_config(|config| {
        config.syllable_rules = SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);
    });
    assert!(!widened.adopt("food"));
    assert!(widened.adopt("zô"));
    widened.process_char('s');
    assert_eq!(widened.buffer(), "zố");
}

fn engine_with_trigger(trigger: &str) -> Engine {
    let mut engine = engine(InputMethod::Telex);
    engine.add_shortcut(trigger, "cơm tấm Sài Gòn");
    engine
}

/// A gõ tắt trigger is no syllable, yet Backspace must re-open its start so a trigger
/// mistyped before Space can be finished (`ctss` + Space, ⌫⌫, `g` + Space).
#[test]
fn adopts_the_start_of_a_shortcut_trigger() {
    let mut engine = engine_with_trigger("ctsg");
    assert!(engine.adopt("cts"));
    assert_eq!(engine.buffer(), "cts");
    assert_eq!(engine.keys(), "cts");
    engine.process_char('g');
    assert_eq!(engine.process_char(' ').output, "cơm tấm Sài Gòn ");
}

#[test]
fn adopts_a_whole_shortcut_trigger() {
    let mut engine = engine_with_trigger("ctsg");
    assert!(engine.adopt("ctsg"));
    assert_eq!(engine.process_char(' ').output, "cơm tấm Sài Gòn ");
}

/// `starts_with` runs from the trigger's side: a word re-opens when a trigger begins
/// with it, never because it begins with a trigger.
#[test]
fn a_one_letter_trigger_reopens_only_itself() {
    let mut engine = engine_with_trigger("k");
    assert!(engine.adopt("k"));
    assert!(engine.adopt("K"));
    assert!(!engine.adopt("kx"));
    assert!(!engine.adopt("x"));
}

#[test]
fn a_trigger_spelled_in_vietnamese_reopens_from_its_screen_form() {
    let mut engine = engine(InputMethod::Telex);
    engine.add_shortcut("đc", "được");
    assert!(engine.adopt("đ"));
    engine.process_char('c');
    assert_eq!(engine.process_char(' ').output, "được ");
}

/// Known limit: only the text on screen is matched, so a trigger stored as the keys
/// that compose it (`ddc`, shown `đc`) cannot be finished this way.
#[test]
fn a_trigger_stored_as_keys_is_not_matched_by_its_screen_form() {
    let mut engine = engine(InputMethod::Telex);
    engine.add_shortcut("ddc", "được");
    assert!(!engine.adopt("đc"));
}

#[test]
fn a_mixed_case_trigger_reopens_from_its_exact_start() {
    let mut engine = engine(InputMethod::Telex);
    engine.add_shortcut("iOS", "iPhone OS");
    assert!(engine.adopt("iO"));
    engine.process_char('S');
    assert_eq!(engine.process_char(' ').output, "iPhone OS ");
}

#[test]
fn vni_reopens_a_trigger_start_too() {
    let mut engine = engine(InputMethod::Vni);
    engine.add_shortcut("ctsg", "cơm tấm Sài Gòn");
    assert!(engine.adopt("cts"));
    engine.process_char('g');
    assert_eq!(engine.process_char(' ').output, "cơm tấm Sài Gòn ");
}

/// Typed fresh, `500k` is a number with a unit and never expands; re-opening `500`
/// must not turn it into a trigger after all.
#[test]
fn a_word_opening_with_a_digit_is_not_reopened_for_a_trigger() {
    let mut engine = engine(InputMethod::Vni);
    engine.add_shortcut("500k", "năm trăm nghìn");
    assert!(!engine.adopt("500"));
}

/// Hosts hand over a single word; text holding a boundary is refused even when a
/// trigger with a space in it would match.
#[test]
fn text_with_a_word_boundary_is_not_reopened_for_a_trigger() {
    let mut engine = engine(InputMethod::Telex);
    engine.add_shortcut("x y", "xy");
    assert!(!engine.adopt("x y"));
    assert!(!engine.adopt("x."));
}

#[test]
fn refuses_a_word_no_trigger_starts_with() {
    let mut engine = engine_with_trigger("ctsg");
    for text in ["ctx", "ctsgx", "hello"] {
        assert!(!engine.adopt(text), "should not adopt {text:?}");
    }
}

/// The start of a trigger matches the way the trigger itself would: smart case folds
/// `Cts`/`CTS`, and with it off only the exact spelling counts.
#[test]
fn a_trigger_start_follows_smart_case() {
    let mut smart = engine_with_trigger("ctsg");
    assert!(smart.adopt("Cts"));
    assert!(smart.adopt("CTS"));
    assert!(!smart.adopt("cTs"));

    let mut exact = engine_with_trigger("ctsg");
    exact.update_config(|config| config.shortcut_smart_case = false);
    assert!(!exact.adopt("Cts"));
    assert!(exact.adopt("cts"));
}

#[test]
fn a_trigger_start_is_refused_while_shortcuts_are_off() {
    let mut engine = engine_with_trigger("ctsg");
    engine.update_config(|config| config.shortcuts_enabled = false);
    assert!(!engine.adopt("cts"));
}

/// English mode re-opens a trigger start only while gõ tắt runs in English too, and
/// never a syllable: there is nothing to retone with Vietnamese off.
#[test]
fn english_mode_adopts_only_a_trigger_start_and_only_with_english_shortcuts() {
    let mut engine = engine_with_trigger("ctsg");
    engine.set_enabled(false);
    assert!(!engine.adopt("chào"));
    assert!(engine.adopt("cts"));
    engine.process_char('g');
    assert_eq!(engine.process_char(' ').output, "cơm tấm Sài Gòn ");

    engine.update_config(|config| config.shortcuts_in_english = false);
    assert!(!engine.adopt("cts"));
}

/// Undo an expansion, then delete the space it left: the trigger re-opens, and the
/// one-time refusal still holds, so the next Space keeps the trigger as typed.
#[test]
fn a_refused_trigger_stays_refused_when_reopened() {
    let mut engine = engine_with_trigger("ctsg");
    for key in "ctsg".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.process_char(' ').output, "cơm tấm Sài Gòn ");
    assert_eq!(engine.undo_shortcut().expect("armed").output, "ctsg ");
    assert!(engine.adopt("ctsg"));
    assert_eq!(engine.process_char(' ').action, funput_engine::Action::None);
}
