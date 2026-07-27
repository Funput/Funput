use funput_core::{InputMethod, ToneStyle, TransformKind, TransformResult, apply};

fn type_keys(keys: &str) -> String {
    crate::support::type_keys(InputMethod::Telex, keys)
}

#[test]
fn telex_stroke_and_tone_basics() {
    assert_eq!(type_keys("dd"), "đ");
    assert_eq!(type_keys("DD"), "Đ");
    assert_eq!(type_keys("as"), "á");
    assert_eq!(type_keys("af"), "à");
    assert_eq!(type_keys("ar"), "ả");
    assert_eq!(type_keys("ax"), "ã");
    assert_eq!(type_keys("aj"), "ạ");
    assert_eq!(type_keys("mas"), "má");
}

#[test]
fn telex_shape_basics() {
    assert_eq!(type_keys("aa"), "â");
    assert_eq!(type_keys("ee"), "ê");
    assert_eq!(type_keys("oo"), "ô");
    assert_eq!(type_keys("ow"), "ơ");
    assert_eq!(type_keys("uw"), "ư");
    assert_eq!(type_keys("aw"), "ă");
    assert_eq!(type_keys("uow"), "uơ");
    assert_eq!(type_keys("uowr"), "uở");
    assert_eq!(type_keys("thuowr"), "thuở");
    assert_eq!(type_keys("thuowngf"), "thường");
    assert_eq!(type_keys("quowis"), "quới");
}

#[test]
fn telex_shape_then_tone() {
    assert_eq!(type_keys("oos"), "ố");
    assert_eq!(type_keys("aas"), "ấ");
}

#[test]
fn telex_reposition() {
    assert_eq!(type_keys("hoaf"), "hòa");
    assert_eq!(type_keys("thuyr"), "thủy");
}

#[test]
fn telex_shape_switch() {
    // A shape key aimed at a vowel that already carries a *different* shape
    // switches it, rather than stalling and dropping the syllable back to raw
    // keys. `w` is the trần/móc key, `a`/`o` the mũ keys for their base letter.
    assert_eq!(type_keys("awa"), "â");
    assert_eq!(type_keys("aaw"), "ă");
    assert_eq!(type_keys("owo"), "ô");
    assert_eq!(type_keys("oow"), "ơ");

    // The switch key may sit anywhere in the syllable, and the tone rides along.
    assert_eq!(type_keys("chawnja"), "chận"); // chặn + a
    assert_eq!(type_keys("chanajw"), "chặn"); // chận + w
    assert_eq!(type_keys("chaajw"), "chặ");
    assert_eq!(type_keys("conwo"), "côn");
    assert_eq!(type_keys("conow"), "cơn");

    // Only shapes the base letter actually has: `ê` takes neither trần nor móc,
    // so `w` stays a literal key here (the engine restores the raw run).
    assert_eq!(type_keys("eew"), "êw");

    // The *same* key twice is still the revert, not a switch.
    assert_eq!(type_keys("aaa"), "aa");
    assert_eq!(type_keys("aww"), "aw");
    assert_eq!(type_keys("uww"), "uw");
    assert_eq!(type_keys("chanaa"), "chana");
}

#[test]
fn telex_revert() {
    // Double modifier restores raw keystrokes: strip diacritic + append the key.
    assert_eq!(type_keys("ass"), "as");
    assert_eq!(type_keys("aaa"), "aa");
    assert_eq!(type_keys("ddd"), "dd");
    assert_eq!(type_keys("aas"), "ấ"); // single sắc on â — not a revert
    assert_eq!(type_keys("aass"), "âs");
    assert_eq!(type_keys("hoaff"), "hoaf");
}

#[test]
fn telex_multi_syllable_words() {
    assert_eq!(
        crate::support::type_words(InputMethod::Telex, "xins chaof banj"),
        "xín chào bạn"
    );
}
#[test]
fn telex_complex_syllables() {
    assert_eq!(type_keys("truowng"), "trương");
    assert_eq!(type_keys("nguwowif"), "người");
    assert_eq!(type_keys("vietj"), "việt");
    assert_eq!(type_keys("truwownfg"), "trường");
    assert_eq!(type_keys("nuocws"), "nước");
}

#[test]
fn telex_free_position_marks() {
    // Marks can be typed anywhere in the syllable, not only adjacent to their
    // target — the user may place the dấu at any position.
    // Breve typed after the coda: "lamws" → lắm (a→ă via w, then sắc via s).
    assert_eq!(type_keys("lamws"), "lắm");
    // Conventional order still works: "lawms" → lắm.
    assert_eq!(type_keys("lawms"), "lắm");
    // Stroke đ typed after the whole rhyme: "duocwjd" → được.
    assert_eq!(type_keys("duocwjd"), "được");
    assert_eq!(type_keys("dduocwj"), "được"); // đ first — unchanged
    // Horn after the coda.
    assert_eq!(type_keys("conw"), "cơn");
    assert_eq!(type_keys("anw"), "ăn");
}

#[test]
fn telex_free_position_circumflex() {
    for (canonical, free, expected) in [
        ("chaan", "chana", "chân"),
        ("deem", "deme", "dêm"),
        ("hoom", "homo", "hôm"),
    ] {
        assert_eq!(type_keys(canonical), expected);
        assert_eq!(type_keys(free), expected);
    }

    for (tone_after, tone_before, expected) in [
        ("chanas", "chasna", "chấn"),
        ("chanaf", "chafna", "chần"),
        ("chanar", "charna", "chẩn"),
        ("chanax", "chaxna", "chẫn"),
        ("chanaj", "chajna", "chận"),
    ] {
        assert_eq!(type_keys(tone_after), expected);
        assert_eq!(type_keys(tone_before), expected);
    }

    // A tone may be pending before the first vowel and becomes unambiguous once
    // the free-position circumflex resolves the syllable.
    assert_eq!(type_keys("chfana"), "chần");

    // A stop coda without a tone is reachable, not complete: the later sắc key
    // must still be able to finish the syllable.
    assert_eq!(type_keys("chatas"), "chất");

    // Open rhymes and two-letter codas use the same resolver as simple codas.
    assert_eq!(type_keys("asa"), "ấ");
    assert_eq!(type_keys("efe"), "ề");
    assert_eq!(type_keys("oso"), "ố");
    assert_eq!(type_keys("kenhe"), "kênh");
    assert_eq!(type_keys("congo"), "công");

    // The mũ may land on a vowel that is not the one already carrying the tone:
    // in `uyê` the tone belongs on `ê`, so the free-position circumflex has to
    // pull it across ("duyjete" → duyệt, not duỵêt).
    for (tone_after, tone_before, expected) in [
        ("duyeetj", "duyjete", "duyệt"),
        ("chuyeenj", "chuyjene", "chuyện"),
        ("tuyeets", "tuysete", "tuyết"),
        ("nguyeenx", "nguyxene", "nguyễn"),
    ] {
        assert_eq!(type_keys(tone_after), expected);
        assert_eq!(type_keys(tone_before), expected);
    }

    // A repeated tone key still undoes the tone once the mark has been pulled
    // across, and every order of the pulled mark converges on the same undo.
    for keys in ["duyeetjj", "duyjetej", "duyjeetj", "duyejtej"] {
        assert_eq!(type_keys(keys), "duyêtj", "{keys}");
    }

    assert_eq!(type_keys("Chana"), "Chân");
    assert_eq!(type_keys("CHANA"), "CHÂN");
    assert_eq!(type_keys("chanaa"), "chana");
}

#[test]
fn telex_free_position_circumflex_keeps_existing_regressions() {
    assert_eq!(type_keys("booong"), "boong");
}

#[test]
fn telex_validation_and_pass_through() {
    // A tone letter with no vowel to land on is kept literally, not dropped.
    assert_eq!(
        apply("ng", 's', InputMethod::Telex, ToneStyle::Traditional),
        TransformResult {
            kind: TransformKind::Pending,
            text: "ngs".into(),
        }
    );
    assert_eq!(
        apply("text", 's', InputMethod::Telex, ToneStyle::Traditional),
        TransformResult {
            kind: TransformKind::Pending,
            text: "texts".into(),
        }
    );
    // Leading `f`/`j` and English words keep every keystroke (engine restores).
    assert_eq!(type_keys("file"), "file");
    assert_eq!(type_keys("from"), "from");
    assert_eq!(type_keys("just"), "just");
    assert_eq!(
        apply("a", 'b', InputMethod::Telex, ToneStyle::Traditional),
        TransformResult {
            kind: TransformKind::Pending,
            text: "ab".into(),
        }
    );
}
