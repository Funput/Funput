//! Public `Engine` API: end-to-end behavior of the frozen surface (composition,
//! backspace, word boundaries, spell-check, auto-capitalize, and the flip hotkey).
//!
//! These exercise only the public API, so they live here as integration tests
//! rather than inline in `src/lib.rs`.

use funput_core::{InputMethod, ToneStyle};
use funput_engine::{Action, Engine, EngineConfig, ImeResult, KeySource};

#[test]
fn engine_new_defaults() {
    let engine = Engine::new();
    assert!(engine.is_enabled());
    assert_eq!(engine.method(), InputMethod::Telex);
    assert_eq!(engine.buffer(), "");
}

/// Footprint headline for benchmarks: the engine's stack size stays tiny (run
/// `cargo test -p funput-engine -- --nocapture engine_struct_size` to print it).
#[test]
fn engine_struct_size() {
    let bytes = std::mem::size_of::<Engine>();
    println!("size_of::<Engine>() = {bytes} bytes");
    assert!(
        bytes < 1024,
        "engine struct unexpectedly large: {bytes} bytes"
    );
}

#[test]
fn set_method_vni() {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Vni);
    assert_eq!(engine.method(), InputMethod::Vni);
}

#[test]
fn set_enabled_false() {
    let mut engine = Engine::new();
    engine.set_enabled(false);
    assert!(!engine.is_enabled());
}

/// Type a word with smart restore off so the spell-check gate is the only thing
/// that can alter the diacritic (eager restore would otherwise mask it).
fn type_word(engine: &mut Engine, word: &str) -> String {
    engine.clear();
    for key in word.chars() {
        engine.process_char(key);
    }
    engine.buffer().to_string()
}

#[test]
fn spell_check_off_keeps_legacy_diacritic() {
    let mut engine = Engine::new();
    engine.update_config(|c| c.smart_restore = false);
    // Default: spell-check off → `tetf` composes `tèt` (huyền) as before, even
    // though a stop coda may only carry sắc / nặng.
    assert_eq!(type_word(&mut engine, "tetf"), "tèt");
}

#[test]
fn spell_check_on_blocks_invalid_syllable() {
    let mut engine = Engine::new();
    engine.update_config(|c| c.smart_restore = false);
    engine.update_config(|c| c.spell_check = true);
    // `tèt` is not a real syllable → the huyền key stays a literal: `tetf`.
    assert_eq!(type_word(&mut engine, "tetf"), "tetf");
    // Real syllables are unaffected.
    assert_eq!(type_word(&mut engine, "mas"), "má");
    assert_eq!(type_word(&mut engine, "tets"), "tét");
}

// ---- Auto-capitalize ("Tự động viết hoa") ----

fn engine_autocap() -> Engine {
    let mut e = Engine::new();
    e.update_config(|c| c.auto_capitalize = true);
    e
}

/// Feed a string keystroke-by-keystroke; return the current composition buffer.
fn feed(engine: &mut Engine, s: &str) -> String {
    for k in s.chars() {
        engine.process_char(k);
    }
    engine.buffer().to_string()
}

#[test]
fn autocap_focus_capitalizes_first_word() {
    let mut e = engine_autocap();
    e.arm_capitalization();
    assert_eq!(feed(&mut e, "viet"), "Viet");
}

#[test]
fn autocap_first_letter_composes_vietnamese() {
    let mut e = engine_autocap();
    e.arm_capitalization();
    assert_eq!(feed(&mut e, "chaof"), "Chào"); // Telex
    e.clear();
    e.arm_capitalization();
    assert_eq!(feed(&mut e, "dd"), "Đ"); // capital đ
}

#[test]
fn autocap_after_sentence_end_and_space() {
    let mut e = engine_autocap();
    feed(&mut e, "ok");
    e.process_char('.'); // commits "ok", marks sentence end
    e.process_char(' '); // whitespace confirms → arm
    assert_eq!(feed(&mut e, "lam"), "Lam");
}

#[test]
fn autocap_requires_whitespace_after_period() {
    let mut e = engine_autocap();
    feed(&mut e, "google");
    e.process_char('.'); // no whitespace follows
    assert_eq!(feed(&mut e, "com"), "com"); // google.com stays lower
}

#[test]
fn autocap_newline_arms() {
    let mut e = engine_autocap();
    feed(&mut e, "ok");
    e.process_char('\n');
    assert_eq!(feed(&mut e, "lam"), "Lam");
}

#[test]
fn autocap_comma_does_not_arm() {
    let mut e = engine_autocap();
    feed(&mut e, "ok");
    e.process_char(',');
    e.process_char(' ');
    assert_eq!(feed(&mut e, "lam"), "lam");
}

#[test]
fn autocap_closing_quote_is_transparent() {
    let mut e = engine_autocap();
    feed(&mut e, "di");
    e.process_char('.'); // sentence end
    e.process_char('"'); // transparent closer
    e.process_char(' '); // arm
    assert_eq!(feed(&mut e, "roi"), "Roi");
}

#[test]
fn autocap_off_is_noop() {
    let mut e = Engine::new(); // default off
    e.arm_capitalization(); // no-op while off
    assert_eq!(feed(&mut e, "viet"), "viet");
    e.process_char('.');
    e.process_char(' ');
    assert_eq!(feed(&mut e, "lam"), "lam");
}

#[test]
fn backspace_keeps_composition_context() {
    // Typo "Phua", backspace the "a", then "s" → "Phú" (tone applies on "Phu").
    let mut engine = Engine::new();
    for key in "Phua".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "Phua");

    let bs = engine.on_backspace();
    assert_eq!(bs.action, Action::None);
    assert_eq!(engine.buffer(), "Phu");
    assert_eq!(engine.keys(), "Phu");

    let result = engine.process_char('s');
    assert_eq!(result.action, Action::Send);
    assert_eq!(engine.buffer(), "Phú");
}

#[test]
fn clear_smoke() {
    let mut engine = Engine::new();
    engine.clear();
    assert_eq!(engine.buffer(), "");
}

#[test]
fn process_char_pending_updates_buffer_and_keys() {
    let mut engine = Engine::new();
    let result = engine.process_char('a');
    assert_eq!(result.action, Action::None);
    assert_eq!(result.backspace, 0);
    assert!(result.output.is_empty());
    assert_eq!(engine.buffer(), "a");
    assert_eq!(engine.keys(), "a");
}

#[test]
fn disabled_does_not_touch_buffer_or_keys() {
    let mut engine = Engine::new();
    engine.set_enabled(false);
    let result = engine.process_char('a');
    assert_eq!(result.action, Action::None);
    assert_eq!(engine.buffer(), "");
    assert_eq!(engine.keys(), "");
}

#[test]
fn word_boundary_clears_after_word() {
    let mut engine = Engine::new();
    engine.process_char('m');
    engine.process_char('a');
    let tone = engine.process_char('s');
    assert_eq!(tone.action, Action::Send);
    assert_eq!(engine.buffer(), "má");
    assert_eq!(engine.keys(), "mas");

    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None);
    assert_eq!(engine.buffer(), "");
    assert_eq!(engine.keys(), "");
}

#[test]
fn vni_keeps_composed_word_instead_of_exposing_digits() {
    // VNI: d-9-c composes "đc". It is not a complete syllable, but reverting
    // would surface the modifier digit ("d9c"), so the composed "đc" is kept.
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Vni);
    for key in "d9c".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "đc");

    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None); // no restore → "đc" committed as-is
    assert_eq!(engine.buffer(), "");
}

#[test]
fn telex_keeps_abbreviation_with_d_stroke() {
    // Telex: G-D-D composes "GĐ" (Giám đốc). Reverting would give "GDD"; the đ
    // marks it as intentional Vietnamese, so it is kept across methods.
    let mut engine = Engine::new(); // Telex by default
    for key in "GDD".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "GĐ");

    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None);
    assert_eq!(engine.buffer(), "");
}

#[test]
fn word_boundary_on_empty_buffer() {
    let mut engine = Engine::new();
    let result = engine.process_char(' ');
    assert_eq!(result.action, Action::None);
    assert_eq!(engine.buffer(), "");
    assert_eq!(engine.keys(), "");
}

#[test]
fn word_boundary_does_not_append_keys() {
    let mut engine = Engine::new();
    engine.process_char('a');
    assert_eq!(engine.keys(), "a");
    engine.process_char(' ');
    assert_eq!(engine.keys(), "");
}

#[test]
fn flip_eager_restored_word_to_vietnamese_and_back() {
    // "card" eager-restores to English mid-word (shown as "card"). Flipping
    // recovers the Vietnamese composition; flipping again returns to raw.
    let mut engine = Engine::new();
    type_word(&mut engine, "card");
    assert_eq!(engine.buffer(), "card"); // shown as raw English

    // Flip → a Send that rewrites the visible word to the Vietnamese form.
    let to_vn = engine.flip_composing();
    assert_eq!(to_vn.action, Action::Send);
    let vn = engine.buffer().to_string();
    assert_ne!(vn, "card");
    // The diff applied to "card" must reproduce the new buffer.
    assert_eq!(apply_diff("card", &to_vn), vn);

    let to_raw = engine.flip_composing();
    assert_eq!(to_raw.action, Action::Send);
    assert_eq!(engine.buffer(), "card"); // back to raw
    assert_eq!(apply_diff(&vn, &to_raw), "card");
}

/// Apply a `Send` result's `backspace`/`output` to `shown` — what a host that
/// types real text (Windows) would end up displaying.
fn apply_diff(shown: &str, result: &ImeResult) -> String {
    let mut chars: Vec<char> = shown.chars().collect();
    chars.truncate(chars.len() - result.backspace);
    chars.into_iter().chain(result.output.chars()).collect()
}

#[test]
fn flip_to_vietnamese_sticks_across_word_boundary() {
    // After flipping "card" to Vietnamese, Space must not restore it to English.
    let mut engine = Engine::new();
    type_word(&mut engine, "card");
    engine.flip_composing();
    let vn = engine.buffer().to_string();

    let boundary = engine.process_char(' ');
    // No restore fired (the flip is sticky), so the boundary just passes through.
    assert_eq!(boundary.action, Action::None);
    // The next composition starts clean.
    assert_eq!(engine.buffer(), "");
    assert_ne!(vn, "card");
}

#[test]
fn flip_kept_vietnamese_word_to_raw() {
    // "má" is valid Vietnamese; flipping shows the raw keys "mas".
    let mut engine = Engine::new();
    type_word(&mut engine, "mas");
    assert_eq!(engine.buffer(), "má");
    assert_eq!(engine.flip_composing().action, Action::Send);
    assert_eq!(engine.buffer(), "mas");
}

#[test]
fn flip_is_noop_without_a_flippable_word() {
    let mut engine = Engine::new();
    assert_eq!(engine.flip_composing().action, Action::None); // nothing composing
    type_word(&mut engine, "the"); // composes to itself — no VN/raw distinction
    assert_eq!(engine.flip_composing().action, Action::None);
    assert_eq!(engine.buffer(), "the");
}

#[test]
fn flip_choice_resets_after_the_word_commits() {
    // The override is per-word: a fresh word restores normally again.
    let mut engine = Engine::new();
    type_word(&mut engine, "card");
    engine.flip_composing(); // force Vietnamese
    engine.process_char(' '); // commit + clear
    type_word(&mut engine, "card"); // a new word
    assert_eq!(engine.buffer(), "card"); // eager-restored again, override gone
}

// --- Word-start digits (numeric fields / OTP codes / phone numbers) ----------

/// A digit typed with an empty buffer is a number, not the start of a Vietnamese
/// syllable, so the engine passes it through untouched in either method.
#[test]
fn word_start_digit_passes_through() {
    for method in [InputMethod::Telex, InputMethod::Vni] {
        let mut engine = Engine::new();
        engine.set_method(method);
        let result = engine.process_char('1');
        assert_eq!(result.action, Action::None, "{method:?}");
        assert_eq!(
            engine.buffer(),
            "",
            "{method:?}: a leading digit must not compose"
        );
    }
}

/// A whole number never opens a composition — including in VNI, where digits are the
/// tone/shape keys (they only modify a preceding vowel, which a bare number lacks).
#[test]
fn full_number_never_composes() {
    for method in [InputMethod::Telex, InputMethod::Vni] {
        let mut engine = Engine::new();
        engine.set_method(method);
        for key in "0912345678".chars() {
            assert_eq!(engine.process_char(key).action, Action::None, "{method:?}");
        }
        assert_eq!(engine.buffer(), "", "{method:?}");
    }
}

/// The rule is word-start only: a VNI tone digit *after* a vowel still applies.
#[test]
fn digit_after_vowel_still_modifies_in_vni() {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Vni);
    engine.process_char('a');
    let result = engine.process_char('1'); // á
    assert_eq!(result.action, Action::Send);
    assert_eq!(engine.buffer(), "á");
}

/// A real word typed right after a leading number composes normally.
#[test]
fn word_after_leading_digit_still_composes() {
    let mut engine = Engine::new(); // Telex by default
    engine.process_char('3'); // passthrough — buffer stays empty
    assert_eq!(engine.buffer(), "");
    for key in "meof".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "mèo");
}

// ---- Numpad digits stay literal numbers (VNI) ----

fn vni_engine() -> Engine {
    let mut e = Engine::new();
    e.set_method(InputMethod::Vni);
    e
}

/// A numpad digit after a vowel is a literal number, not a tone: it commits the
/// word and passes the number through, unlike the top-row digit which adds nặng.
#[test]
fn vni_numpad_digit_is_literal_not_tone() {
    let mut engine = vni_engine();
    engine.process_char('a'); // composing "a"
    let result = engine.process_key('5', KeySource::Numpad);
    assert_eq!(engine.buffer(), ""); // committed; no nặng applied
    assert_eq!(result.action, Action::None); // number passes through to the app
}

/// Same keystroke from the main keyboard keeps its VNI-modifier role.
#[test]
fn vni_top_row_digit_still_applies_tone() {
    let mut engine = vni_engine();
    engine.process_char('a');
    engine.process_key('5', KeySource::Standard); // == process_char('5')
    assert_eq!(engine.buffer(), "ạ");
}

/// A numpad digit commits a finished word untouched (no tone reshuffle).
#[test]
fn vni_numpad_digit_commits_current_word() {
    let mut engine = vni_engine();
    for key in ['c', 'h', 'a', 'o'] {
        engine.process_char(key);
    }
    engine.process_char('2'); // huyền → "chào"
    assert_eq!(engine.buffer(), "chào");
    let result = engine.process_key('5', KeySource::Numpad);
    assert_eq!(engine.buffer(), ""); // "chào" committed, "5" is literal
    assert_eq!(result.action, Action::None);
}

/// The rule holds for every VNI digit role, including `0` (remove-tone) and the
/// shape digits `6`–`8`.
#[test]
fn vni_numpad_covers_all_digit_roles() {
    for digit in ['0', '6', '7', '8', '9'] {
        let mut engine = vni_engine();
        engine.process_char('a');
        engine.process_key(digit, KeySource::Numpad);
        assert_eq!(engine.buffer(), "", "numpad {digit} should not modify 'a'");
    }
}

// ---- configure(): the batch config API (mirrors the individual set_* methods) ----

#[test]
fn configure_applies_all_options() {
    let mut engine = Engine::new();
    engine.configure(EngineConfig {
        method: InputMethod::Vni,
        tone_style: ToneStyle::Modern,
        smart_restore: false,
        eager_restore: false,
        spell_check: true,
        auto_capitalize: true,
        shortcuts_enabled: false,
        shortcut_smart_case: false,
    });
    assert_eq!(engine.method(), InputMethod::Vni);
    assert_eq!(engine.tone_style(), ToneStyle::Modern);
    let config = engine.config();
    assert_eq!(config.method, InputMethod::Vni);
    assert!(config.spell_check && config.auto_capitalize);
    assert!(!config.smart_restore && !config.eager_restore);
    assert!(!config.shortcuts_enabled && !config.shortcut_smart_case);
}

/// `configure` keeps the `set_*` side effect: switching method mid-word clears the
/// in-progress composition.
#[test]
fn configure_method_change_clears_composition() {
    let mut engine = Engine::new(); // Telex
    for key in "chaof".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "chào");
    engine.configure(EngineConfig {
        method: InputMethod::Vni,
        ..EngineConfig::default()
    });
    assert_eq!(engine.buffer(), ""); // method change discarded the old-grammar word
}
