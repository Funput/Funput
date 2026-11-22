//! Retone after Backspace, driven the way a hook + inject shell drives it.
//!
//! [`Shell`] is the Windows hook's `handle_keydown` with the OS taken out; what the
//! tests assert is the text the user ends up looking at.

mod support;

use funput_core::InputMethod;
use support::Shell;

/// The feature as the user asked for it: `phủ`, Space, Backspace, then a tone key
/// edits the word instead of typing a letter after it.
#[test]
fn backspace_after_a_space_retones_the_word() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("phur ");
    assert_eq!(shell.app, "phủ ");

    shell.backspace();
    assert_eq!(shell.app, "phủ", "the space is gone, the word is untouched");

    shell.key('s');
    assert_eq!(shell.app, "phú");
}

#[test]
fn retoning_works_the_same_in_vni() {
    let mut shell = Shell::new(InputMethod::Vni);
    shell.type_str("phu3 ");
    assert_eq!(shell.app, "phủ ");

    shell.backspace();
    shell.key('1'); // VNI sắc
    assert_eq!(shell.app, "phú");
}

/// The retoned word is a normal composition again: the next boundary commits the
/// edited form, and English restore does not fire on it.
#[test]
fn a_boundary_after_retoning_commits_the_edited_word() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("chaof ");
    shell.backspace();
    shell.key('s');
    shell.key(' ');
    assert_eq!(shell.app, "cháo ");
}

/// Two words: only the one the caret lands on is re-opened, and the text in front
/// of it is left alone.
#[test]
fn only_the_word_at_the_caret_is_reopened() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("chaof phur ");
    assert_eq!(shell.app, "chào phủ ");

    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "chào phú");
}

/// The reported bug: a word whose stop coda (`c`, `ch`, `p`, `t`) never got its tone
/// is committed exactly as typed, so re-opening it has to work too.
#[test]
fn a_word_committed_before_its_tone_is_reopened() {
    for (keys, tone, expected) in [
        ("chuc ", 's', "chúc"),
        ("tich ", 's', "tích"),
        ("hoc ", 'j', "học"),
        ("dat ", 'j', "dạt"),
    ] {
        let mut shell = Shell::new(InputMethod::Telex);
        shell.type_str(keys);
        shell.backspace();
        shell.key(tone);
        assert_eq!(shell.app, expected, "typing {keys:?} then ⌫ then {tone:?}");
    }
}

/// The same bug one step earlier: a word committed before its *vowel shape*. `dien`
/// is `diên` without the circumflex, so the shape, stroke and tone keys all have to
/// reach it — `dien` + Space + ⌫ + `d` gives `đien`, not `diend`.
#[test]
fn a_word_committed_before_its_vowel_shape_is_reopened() {
    for (keys, modifier, expected) in [
        ("dien ", 'd', "đien"),
        ("vien ", 'e', "viên"),
        ("thuo ", 'w', "thuơ"),
        ("nuoc ", 'w', "nươc"),
        ("cuu ", 'w', "cưu"),
    ] {
        let mut shell = Shell::new(InputMethod::Telex);
        shell.type_str(keys);
        shell.backspace();
        shell.key(modifier);
        assert_eq!(
            shell.app, expected,
            "typing {keys:?} then ⌫ then {modifier:?}"
        );
    }
}

/// VNI reaches a re-opened word the same way — the gate is the engine's, not Telex's.
#[test]
fn a_word_awaiting_its_shape_is_reopened_in_vni_too() {
    let mut shell = Shell::new(InputMethod::Vni);
    shell.type_str("vien ");
    shell.backspace();
    shell.key('6'); // VNI circumflex
    assert_eq!(shell.app, "viên");
}

/// The looser gate stops at the word boundary: an English word that happens to look
/// like an unshaped rhyme is still restored when the next boundary arrives.
#[test]
fn an_english_word_that_looks_unshaped_is_restored_at_the_boundary() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("true ");
    assert_eq!(shell.app, "true ");

    shell.backspace();
    shell.key('s');
    shell.key(' ');
    assert_eq!(shell.app, "trues ");
}

/// The engine only adopts real Vietnamese syllables, so an English word keeps
/// taking literal letters after it.
#[test]
fn an_english_word_is_not_reopened() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("hello ");
    assert_eq!(shell.app, "hello ");

    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "hellos");
}

/// Every character between the word and the caret has to be deleted first — the
/// word is only re-opened once the caret actually reaches it.
#[test]
fn punctuation_between_the_word_and_the_caret_is_deleted_first() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("phur, ");
    assert_eq!(shell.app, "phủ, ");

    shell.backspace();
    shell.key('s');
    assert_eq!(
        shell.app, "phủ,s",
        "the caret was on the comma, not on the word"
    );

    shell.flush();
    shell.type_str("phur, ");
    shell.backspace();
    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "phủ,sphú");
}

/// Holding Backspace eats the live word and carries on into the one before it,
/// which is re-opened in turn.
#[test]
fn backspace_carries_on_into_the_previous_word() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("chaof banj");
    assert_eq!(shell.app, "chào bạn");

    for _ in 0..4 {
        shell.backspace(); // ba̱n, ba, b, then the space
    }
    assert_eq!(shell.app, "chào");

    shell.key('s');
    assert_eq!(shell.app, "cháo");
}

/// Anything that may have moved the caret without Funput seeing it — a click, an
/// arrow key, a focus change — drops the shadow, and Backspace goes back to being
/// a plain delete.
#[test]
fn a_flush_disables_retoning() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("phur ");
    shell.flush(); // the user clicked somewhere; Funput no longer knows the caret

    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "phủs");
}

/// Backspace right after a gõ tắt expansion takes it back, boundary and all, so
/// the trigger can be kept as typed (issue #582).
#[test]
fn backspace_right_after_an_expansion_undoes_it() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("vn", "Việt Nam");
    shell.type_str("vn ");
    assert_eq!(shell.app, "Việt Nam ");

    shell.backspace();
    assert_eq!(shell.app, "vn ");
}

/// The undo gives back the word as typed, so smart case survives it.
#[test]
fn the_undo_keeps_the_case_the_trigger_was_typed_in() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("vn", "Việt Nam");
    shell.type_str("Vn ");
    assert_eq!(shell.app, "Việt Nam ");

    shell.backspace();
    assert_eq!(shell.app, "Vn ");
}

/// A second Backspace deletes the space and re-opens the trigger, which the undo
/// left refused once: Space keeps it instead of expanding it again.
#[test]
fn an_undone_trigger_is_kept_when_reopened() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("vn", "Việt Nam");
    shell.type_str("vn ");

    shell.backspace(); // the undo
    shell.backspace(); // the space
    assert_eq!(shell.app, "vn");

    shell.key(' ');
    assert_eq!(shell.app, "vn ");
}

/// Reported in #742: after the undo, deleting the word and typing the trigger again
/// expands it. The refusal was for the word the undo put back, which is gone.
#[test]
fn deleting_an_undone_trigger_and_retyping_it_expands_it() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("omg", "Oh my god");
    shell.type_str("Omg ");
    shell.backspace(); // the undo
    assert_eq!(shell.app, "Omg ");

    for _ in 0..4 {
        shell.backspace();
    }
    assert_eq!(shell.app, "");

    shell.type_str("Omg ");
    assert_eq!(shell.app, "Oh my god ");
}

/// The same for a trigger whose screen form the shell cannot re-open (`ddc` shows
/// as `đc`): deleting into it is enough.
#[test]
fn deleting_an_undone_composed_trigger_and_retyping_it_expands_it() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("ddc", "được");
    shell.type_str("ddc ");
    shell.backspace(); // the undo
    assert_eq!(shell.app, "đc ");

    for _ in 0..3 {
        shell.backspace();
    }
    assert_eq!(shell.app, "");

    shell.type_str("ddc ");
    assert_eq!(shell.app, "được ");
}

/// Any key after the expansion spends the undo: Backspace is plain again.
#[test]
fn a_key_after_the_expansion_spends_the_undo() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("vn", "Việt Nam");
    shell.type_str("vn x");

    shell.backspace();
    assert_eq!(shell.app, "Việt Nam ");
}

/// A click or caret key leaves the expansion behind for good.
#[test]
fn a_caret_move_spends_the_undo() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("vn", "Việt Nam");
    shell.type_str("vn ");
    shell.flush();

    shell.backspace();
    assert_eq!(shell.app, "Việt Nam");
}

/// An expansion longer than the shadow is only partly remembered, so the shell
/// cannot vouch for what is on screen: Backspace deletes one character instead of
/// undoing over text it does not know.
#[test]
fn an_expansion_longer_than_the_shadow_is_not_undone() {
    let long = "lorem ipsum ".repeat(30);
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("li", long.trim_end());
    shell.type_str("li ");
    assert_eq!(shell.app, long);

    shell.backspace();
    assert_eq!(shell.app, long.trim_end());
}

fn shell_with_ct_cf() -> Shell {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("ct", "cơm tấm");
    shell.engine.add_shortcut("cf", "cà phê");
    shell
}

fn backspace_times(shell: &mut Shell, times: usize) {
    for _ in 0..times {
        shell.backspace();
    }
}

/// #782: typed on past an expansion, deleted back to it, and the trigger was the
/// wrong one. Backspace still takes the expansion back, then the trigger is fixed.
#[test]
fn backspace_back_at_an_expansion_after_more_words_undoes_it() {
    let mut shell = shell_with_ct_cf();
    shell.type_str("ct nayf raast ngon");
    assert_eq!(shell.app, "cơm tấm này rất ngon");

    backspace_times(&mut shell, "này rất ngon".chars().count());
    assert_eq!(shell.app, "cơm tấm ");
    shell.backspace();
    assert_eq!(shell.app, "ct ");

    backspace_times(&mut shell, 2);
    shell.type_str("f ");
    assert_eq!(shell.app, "cà phê ");
}

/// The same text typed further on is not the expansion: only its own end counts.
#[test]
fn the_same_text_typed_again_is_not_taken_back() {
    let mut shell = shell_with_ct_cf();
    shell.type_str("ct cowm taams ");
    assert_eq!(shell.app, "cơm tấm cơm tấm ");

    shell.backspace();
    assert_eq!(shell.app, "cơm tấm cơm tấm");
}

/// A caret move in between leaves the expansion behind for good.
#[test]
fn a_caret_move_after_more_words_forgets_the_expansion() {
    let mut shell = shell_with_ct_cf();
    shell.type_str("ct x");
    shell.flush();

    backspace_times(&mut shell, 2);
    assert_eq!(shell.app, "cơm tấm");
}

/// Typed so far on that the shadow dropped the expansion, deleting back to it is
/// plain Backspace: the shell no longer knows the spot.
#[test]
fn an_expansion_the_shadow_dropped_is_not_taken_back() {
    let mut shell = shell_with_ct_cf();
    let filler = "abc ".repeat(80);
    shell.type_str("ct ");
    shell.type_str(&filler);

    backspace_times(&mut shell, filler.chars().count());
    assert_eq!(shell.app, "cơm tấm ");
    shell.backspace();
    assert_eq!(shell.app, "cơm tấm");
}

/// English mode with gõ tắt on finds the expansion the same way.
#[test]
fn english_mode_takes_the_expansion_back_after_more_words() {
    let mut shell = shell_with_ct_cf();
    shell.engine.set_enabled(false);
    shell.type_str("ct is good");
    assert_eq!(shell.app, "cơm tấm is good");

    backspace_times(&mut shell, "is good".chars().count());
    shell.backspace();
    assert_eq!(shell.app, "ct ");
}

/// Reported: `dungh` + Space, then Backspace twice to drop the space and the stray
/// `h`. The caret now sits at the end of `dung`, so the tone key has to reach it.
#[test]
fn a_refused_word_is_reopened_once_backspace_makes_it_a_syllable() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("dungh ");
    assert_eq!(shell.app, "dungh ");

    shell.backspace(); // the space
    shell.backspace(); // the stray `h`
    assert_eq!(shell.app, "dung");

    shell.key('s');
    assert_eq!(shell.app, "dúng");
}

fn shell_with_ctsg() -> Shell {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("ctsg", "cơm tấm Sài Gòn");
    shell
}

/// Reported in #582: a trigger mistyped and committed with Space is fixed by deleting
/// back into it. The start of a trigger re-opens like a syllable does, so the fixed
/// trigger expands at the next Space.
#[test]
fn a_mistyped_trigger_is_fixed_after_space() {
    let mut shell = shell_with_ctsg();
    shell.type_str("ctss ");
    assert_eq!(shell.app, "ctss ");

    shell.backspace(); // the space; `ctss` begins no trigger, so it stays committed
    shell.backspace(); // the stray `s`; `cts` begins `ctsg` and re-opens
    shell.type_str("g ");
    assert_eq!(shell.app, "cơm tấm Sài Gòn ");
}

#[test]
fn a_fixed_trigger_keeps_its_smart_case() {
    let mut shell = shell_with_ctsg();
    shell.type_str("Ctss ");
    shell.backspace();
    shell.backspace();
    shell.type_str("g ");
    assert_eq!(shell.app, "Cơm tấm Sài Gòn ");
}

/// Deleting a stray last key lands on the whole trigger, which re-opens too.
#[test]
fn deleting_a_stray_key_lands_on_the_whole_trigger() {
    let mut shell = shell_with_ctsg();
    shell.type_str("ctsgx ");
    assert_eq!(shell.app, "ctsgx ");

    shell.backspace();
    shell.backspace();
    shell.key(' ');
    assert_eq!(shell.app, "cơm tấm Sài Gòn ");
}

/// The shadow remembers a short phrase, so the trigger can be reached by deleting back
/// over the words typed after it.
#[test]
fn a_trigger_is_reached_back_over_several_words() {
    let mut shell = shell_with_ctsg();
    let after = "abc ".repeat(40); // 160 bytes: past the old 64-byte shadow
    shell.type_str("ctss ");
    shell.type_str(&after);

    for _ in 0..after.chars().count() + 2 {
        shell.backspace(); // every `abc `, then the space and the stray `s`
    }
    assert_eq!(shell.app, "cts");
    shell.type_str("g ");
    assert_eq!(shell.app, "cơm tấm Sài Gòn ");
}

/// The repair still works on a trigger with a word and a space in front of it.
#[test]
fn a_mistyped_trigger_after_another_word_is_fixed_too() {
    let mut shell = shell_with_ctsg();
    shell.type_str("an ctss ");
    shell.backspace();
    shell.backspace();
    shell.type_str("g ");
    assert_eq!(shell.app, "an cơm tấm Sài Gòn ");
}

/// A word Backspace re-opens is glued to what the shadow saw in front of it: the
/// domain that did not expand when typed does not expand on a second Space either.
#[test]
fn a_reopened_word_glued_to_a_full_stop_does_not_expand() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("vn", "Việt Nam");
    shell.type_str("tuoitre.vn ");
    assert_eq!(shell.app, "tuoitre.vn ");

    shell.backspace();
    shell.key(' ');
    assert_eq!(shell.app, "tuoitre.vn ");
}

/// A word that begins no trigger stays as literal as before.
#[test]
fn a_word_no_trigger_starts_with_is_not_reopened() {
    let mut shell = shell_with_ctsg();
    shell.type_str("hello ");
    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "hellos");
}
