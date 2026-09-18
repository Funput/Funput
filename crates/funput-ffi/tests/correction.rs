//! The typo-correction handshake, driven exactly as a keyboard shell drives it.

use funput_ffi::{
    ACTION_NONE, ACTION_SEND, CORRECTION_CAP, FunputCorrectionCandidate, FunputEngine,
    FunputKeyTouch, FunputResult, TOUCH_ALTERNATE_CAP, funput_backspace,
    funput_engine_apply_correction, funput_engine_choose_correction,
    funput_engine_correction_candidates, funput_engine_correction_undo_text, funput_engine_free,
    funput_engine_has_correction_undo, funput_engine_has_pending_correction, funput_engine_new,
    funput_engine_pending_correction_backspace, funput_engine_set_next_key_touch,
    funput_process_char, funput_set_typo_correction,
};

fn output(result: &FunputResult) -> String {
    result.chars[..result.count as usize]
        .iter()
        .filter_map(|&c| char::from_u32(c))
        .collect()
}

fn touch(typed: char, alternate: Option<char>) -> FunputKeyTouch {
    let mut touch = FunputKeyTouch {
        typed: typed as u32,
        typed_distance: 0.35,
        alternates: [0; TOUCH_ALTERNATE_CAP],
        distances: [0.0; TOUCH_ALTERNATE_CAP],
        alternate_count: 0,
    };
    if let Some(alternate) = alternate {
        touch.alternates[0] = alternate as u32;
        touch.distances[0] = 0.15;
        touch.alternate_count = 1;
    }
    touch
}

/// Type `keys` through the C API, mirroring what the app would show, with the key at
/// `slip.0` reported as having landed nearer `slip.1`.
unsafe fn type_word(engine: *mut FunputEngine, app: &mut String, keys: &str, slip: (usize, char)) {
    for (index, key) in keys.chars().enumerate() {
        let reported = touch(key, (index == slip.0).then_some(slip.1));
        unsafe { funput_engine_set_next_key_touch(engine, &raw const reported) };
        let result = unsafe { funput_process_char(engine, key as u32) };
        if result.action == ACTION_NONE {
            app.push(key);
        } else {
            for _ in 0..result.backspace {
                app.pop();
            }
            app.push_str(&output(&result));
        }
    }
}

fn candidates(engine: *const FunputEngine) -> Vec<(String, f32, u32)> {
    let mut out = [FunputCorrectionCandidate::default(); CORRECTION_CAP];
    let len =
        unsafe { funput_engine_correction_candidates(engine, out.as_mut_ptr(), CORRECTION_CAP) };
    out[..len]
        .iter()
        .map(|candidate| {
            let text: String = candidate.chars[..candidate.count as usize]
                .iter()
                .filter_map(|&c| char::from_u32(c))
                .collect();
            (text, candidate.touch_score, candidate.edits)
        })
        .collect()
}

#[test]
fn the_whole_handshake_replaces_the_word_and_keeps_the_boundary() {
    unsafe {
        let engine = funput_engine_new();
        funput_set_typo_correction(engine, true);

        let mut app = String::new();
        type_word(engine, &mut app, "dduwowfnh ", (8, 'g'));
        assert_eq!(app, "dduwowfnh ", "the keystroke itself changed nothing");
        assert!(funput_engine_has_pending_correction(engine));
        assert_eq!(funput_engine_pending_correction_backspace(engine), 10);

        let offered = candidates(engine);
        assert_eq!(offered.len(), 1);
        assert_eq!(offered[0].0, "đường");
        assert!(offered[0].1 < 0.0, "a log-likelihood is negative");
        assert_eq!(offered[0].2, 1);

        let winner = funput_engine_choose_correction(engine, std::ptr::null(), 0);
        assert_eq!(winner, 0);

        let result = funput_engine_apply_correction(engine, winner);
        assert_eq!(result.action, ACTION_SEND);
        for _ in 0..result.backspace {
            app.pop();
        }
        app.push_str(&output(&result));
        assert_eq!(app, "đường ");
        assert!(!funput_engine_has_pending_correction(engine));

        funput_engine_free(engine);
    }
}

#[test]
fn backspace_after_a_correction_undoes_it_through_the_abi() {
    unsafe {
        let engine = funput_engine_new();
        funput_set_typo_correction(engine, true);
        let mut app = String::new();
        type_word(engine, &mut app, "dduwowfnh ", (8, 'g'));
        funput_engine_apply_correction(engine, 0);

        assert!(funput_engine_has_correction_undo(engine));
        let mut text = [0u32; 32];
        let len = funput_engine_correction_undo_text(engine, text.as_mut_ptr(), text.len());
        let typed: String = text[..len]
            .iter()
            .filter_map(|&c| char::from_u32(c))
            .collect();
        assert_eq!(typed, "dduwowfnh");

        let undo = funput_backspace(engine);
        assert_eq!(undo.action, ACTION_SEND);
        assert_eq!(undo.backspace, 6);
        assert_eq!(output(&undo), "dduwowfnh ");
        assert!(!funput_engine_has_correction_undo(engine));

        funput_engine_free(engine);
    }
}

#[test]
fn the_word_store_breaks_a_tie_the_touches_cannot() {
    unsafe {
        let engine = funput_engine_new();
        funput_set_typo_correction(engine, true);
        let mut app = String::new();
        // Both `f` and `s` are offered for the last key, so `nhà` and `nhá` tie.
        for (index, key) in "nhad".chars().enumerate() {
            let mut reported = touch(key, None);
            if index == 3 {
                reported.alternates[0] = 'f' as u32;
                reported.alternates[1] = 's' as u32;
                reported.distances[0] = 0.15;
                reported.distances[1] = 0.15;
                reported.alternate_count = 2;
            }
            funput_engine_set_next_key_touch(engine, &raw const reported);
            funput_process_char(engine, key as u32);
            app.push(key);
        }
        funput_process_char(engine, ' ' as u32);

        assert_eq!(candidates(engine).len(), 2);
        assert_eq!(
            funput_engine_choose_correction(engine, std::ptr::null(), 0),
            -1,
            "too close to call without a word store"
        );

        let uses = [40u32, 0];
        assert_eq!(
            funput_engine_choose_correction(engine, uses.as_ptr(), uses.len()),
            0
        );

        funput_engine_free(engine);
    }
}

#[test]
fn a_host_that_never_switches_it_on_sees_nothing() {
    unsafe {
        let engine = funput_engine_new();
        let mut app = String::new();
        type_word(engine, &mut app, "dduwowfnh ", (8, 'g'));
        assert_eq!(app, "dduwowfnh ");
        assert!(!funput_engine_has_pending_correction(engine));
        assert!(candidates(engine).is_empty());
        assert_eq!(
            funput_engine_apply_correction(engine, 0).action,
            ACTION_NONE
        );
        assert_eq!(funput_backspace(engine).action, ACTION_NONE);
        funput_engine_free(engine);
    }
}

#[test]
fn every_call_is_null_safe() {
    unsafe {
        let null: *mut FunputEngine = std::ptr::null_mut();
        let reported = touch('a', Some('s'));
        funput_engine_set_next_key_touch(null, &raw const reported);
        funput_set_typo_correction(null, true);
        assert!(!funput_engine_has_pending_correction(null));
        assert_eq!(funput_engine_pending_correction_backspace(null), 0);
        assert_eq!(
            funput_engine_choose_correction(null, std::ptr::null(), 0),
            -1
        );
        assert_eq!(funput_engine_apply_correction(null, 0).action, ACTION_NONE);
        assert!(!funput_engine_has_correction_undo(null));
        assert_eq!(
            funput_engine_correction_undo_text(null, std::ptr::null_mut(), 0),
            0
        );

        // A live engine with null buffers must be just as safe.
        let engine = funput_engine_new();
        funput_set_typo_correction(engine, true);
        funput_engine_set_next_key_touch(engine, std::ptr::null());
        assert_eq!(
            funput_engine_correction_candidates(engine, std::ptr::null_mut(), 8),
            0
        );
        assert_eq!(
            funput_engine_correction_undo_text(engine, std::ptr::null_mut(), 8),
            0
        );
        funput_engine_free(engine);
    }
}

#[test]
fn a_touch_that_is_not_a_scalar_is_ignored_rather_than_trusted() {
    unsafe {
        let engine = funput_engine_new();
        funput_set_typo_correction(engine, true);
        let mut reported = touch('h', Some('g'));
        reported.typed = 0xD800; // a surrogate: never a `char`
        funput_engine_set_next_key_touch(engine, &raw const reported);
        funput_process_char(engine, 'h' as u32);
        funput_process_char(engine, ' ' as u32);
        assert!(!funput_engine_has_pending_correction(engine));
        funput_engine_free(engine);
    }
}
