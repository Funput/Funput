use funput_core::{InputMethod, ToneStyle, TransformKind, apply, apply_checked};

fn typed(keys: &str) -> String {
    crate::support::type_keys(InputMethod::TelexAdvanced, keys)
}

#[test]
fn full_telex_shortcuts_compose() {
    for (keys, output) in [
        ("w", "ư"),
        ("W", "Ư"),
        ("wf", "ừ"),
        ("ww", "w"),
        ("WW", "W"),
        ("t[", "tư"),
        ("m]", "mơ"),
        ("tr[]ngf", "trường"),
        ("ng[]if", "người"),
        ("wngf", "ừng"),
        ("WWindowws", "Windows"),
    ] {
        assert_eq!(typed(keys), output, "{keys}");
    }
}

#[test]
fn every_tone_and_removal_work_on_shortcuts() {
    for (key, output) in [('s', "ứ"), ('f', "ừ"), ('r', "ử"), ('x', "ữ"), ('j', "ự")] {
        assert_eq!(typed(&format!("w{key}")), output);
    }
    assert_eq!(typed("wfs"), "ứ");
    assert_eq!(typed("wfz"), "ư");
}

#[test]
fn tone_styles_and_existing_telex_converge() {
    for style in [ToneStyle::Traditional, ToneStyle::Modern] {
        let mut buffer = String::new();
        for key in "tr[]ngf".chars() {
            buffer = apply(&buffer, key, InputMethod::TelexAdvanced, style).text;
        }
        assert_eq!(buffer, "trường");
    }
    for keys in ["chana", "booong"] {
        assert_eq!(
            typed(keys),
            crate::support::type_keys(InputMethod::Telex, keys)
        );
    }
}

#[test]
fn leading_w_covers_the_whole_onset() {
    // `w` is the `ư` key wherever the syllable still lacks a nucleus, not only at
    // position 0 — the same rule `[` already follows.
    for (keys, output) in [
        ("thw", "thư"),
        ("Thw", "Thư"),
        ("THW", "THƯ"),
        ("nhwng", "nhưng"),
        ("thwongf", "thường"),
        ("ngwoif", "người"),
        ("chwa", "chưa"),
        ("thws", "thứ"),
        ("cwuf", "cừu"),
    ] {
        assert_eq!(typed(keys), output, "{keys}");
    }

    // A second `w` puts the literal key back and keeps the onset, so a Latin run
    // stays escapable: `sww` → `sw`, not the bare `w` the old rule produced.
    for (keys, output) in [("sww", "sw"), ("thww", "thw"), ("ww", "w"), ("thWW", "thW")] {
        assert_eq!(typed(keys), output, "{keys}");
    }
}

#[test]
fn leading_w_still_reaches_the_uo_horn() {
    // The leading `w` consumes the key as `ư`, so it can no longer be the pending
    // horn that plain Telex resolves against a later `uo`. The `ưu` re-parse in
    // `uo_horn` covers the gap — the stray `u` is dropped and the compound
    // completes as usual, so these keep composing exactly as in plain Telex.
    for (keys, output) in [
        ("trwuongf", "trường"),
        ("dwduocj", "được"),
        ("thwuong", "thương"),
        ("nwuocs", "nước"),
        ("ngwuowif", "người"),
        ("bwuoms", "bướm"),
        ("chwua", "chưa"),
        ("hwuou", "hươu"),
        ("mwuownj", "mượn"),
    ] {
        assert_eq!(typed(keys), output, "{keys}");
        assert_eq!(
            crate::support::type_keys(InputMethod::Telex, keys),
            output,
            "plain {keys}"
        );
    }

    // The same re-parse fixes the `[` shortcut, which never reached `ươ` before.
    assert_eq!(typed("tr[uongf"), "trường");
    assert_eq!(typed("tr[ongf"), "trường");
    assert_eq!(typed("tr[]ngf"), "trường");

    // `ưu` itself is untouched — it is a closed rhyme, so only a *following*
    // vowel marks the `u` as stray.
    for (keys, output) in [
        ("cwuf", "cừu"),
        ("hwuu", "hưu"),
        ("lwuu", "lưu"),
        ("c[uf", "cừu"),
        ("tr[uf", "trừu"),
    ] {
        assert_eq!(typed(keys), output, "{keys}");
    }
}

#[test]
fn leading_w_skips_the_q_onset() {
    // No syllable reads `qư`, so `w` after a lone `q` stays the ordinary trần.
    assert_eq!(typed("qwuangj"), "quặng");
    assert_eq!(typed("quawngj"), "quặng");
    assert_eq!(typed("quangwj"), "quặng");
}

#[test]
fn leading_w_loses_the_onset_placed_non_uo_horn() {
    // Remaining divergence, documented in docs/features/advanced-telex.md: when a
    // `w` typed straight after the onset was meant as the trần/móc of a *later*
    // vowel that is not the `uo` compound, Full Telex now reads it as `ư`. Those
    // words stay reachable through every other free position (`conw`, `lamws`).
    assert_eq!(typed("cwon"), "cươn"); // plain Telex: cơn
    assert_eq!(typed("lwams"), "lứam"); // plain Telex: lắm
    assert_eq!(typed("nwux"), "nữu"); // plain Telex: nữ
    for (keys, output) in [("conw", "cơn"), ("lamws", "lắm"), ("nuwx", "nữ")] {
        assert_eq!(typed(keys), output, "{keys}");
    }
    // Plain Telex keeps the deferred `w` in every position.
    for (keys, output) in [("cwon", "cơn"), ("lwams", "lắm"), ("nwux", "nữ")] {
        assert_eq!(
            crate::support::type_keys(InputMethod::Telex, keys),
            output,
            "plain {keys}"
        );
    }
}

#[test]
fn standard_methods_keep_shortcuts_literal() {
    assert_eq!(crate::support::type_keys(InputMethod::Telex, "w"), "w");
    assert_eq!(crate::support::type_keys(InputMethod::Telex, "t["), "t[");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "m]"), "m]");
    assert_eq!(typed("{"), "{");
    assert_eq!(typed("}"), "}");
}

#[test]
fn spell_check_falls_back_to_the_raw_shortcut() {
    let result = apply_checked(
        "text",
        '[',
        InputMethod::TelexAdvanced,
        ToneStyle::Traditional,
        true,
    );
    assert_eq!(result.kind, TransformKind::Pending);
    assert_eq!(result.text, "text[");
}
