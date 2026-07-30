use funput_core::{InputMethod, ToneStyle, TransformKind, apply};

#[test]
fn vni_stroke_d9() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "d9"), "đ");
}

#[test]
fn vni_stroke_uppercase_d9() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "D9"), "Đ");
}

#[test]
fn vni_tone_single_vowel() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a1"), "á");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a2"), "à");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a3"), "ả");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a4"), "ã");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a5"), "ạ");
}

#[test]
fn vni_syllables_with_tone() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "ma1"), "má");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "ca2"), "cà");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "ho5"), "họ");
}

#[test]
fn vni_longer_syllables() {
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "trung1"),
        "trúng"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "ban5"), "bạn");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "nem1"), "ném");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "dep1"), "dép");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "sinh1"), "sính");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "nuoc1"), "nuóc");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "dung2"), "dùng");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "thanh1"),
        "thánh"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "nghe1"), "nghé");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "phong1"),
        "phóng"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "dong2"), "dòng");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "thang1"),
        "tháng"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "d9i5nh"),
        "định"
    );
}

#[test]
fn vni_multi_syllable_words() {
    assert_eq!(
        crate::support::type_words(InputMethod::Vni, "xin1 dong2"),
        "xín dòng"
    );
    assert_eq!(
        crate::support::type_words(InputMethod::Vni, "xin1 dong2 cac1 ban5"),
        "xín dòng các bạn"
    );
    assert_eq!(
        crate::support::type_words(InputMethod::Vni, "trung1 quoc1 ban5 nuoc1"),
        "trúng quóc bạn nuóc"
    );
    assert_eq!(
        crate::support::type_words(InputMethod::Vni, "hom do dep1 nem1"),
        "hom do dép ném"
    );
}

#[test]
fn vni_normal_append() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "ab"), "ab");
}

#[test]
fn vni_shape_basic() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a6"), "â");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "o71"), "ớ");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "o1"), "ó");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a61"), "ấ");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a81"), "ắ");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "to6"), "tô");
}

#[test]
fn vni_shape_switch() {
    let keys = |k| crate::support::type_keys(InputMethod::Vni, k);
    // 6/7/8 on a vowel that already carries a different shape switches it.
    assert_eq!(keys("a86"), "â");
    assert_eq!(keys("a68"), "ă");
    assert_eq!(keys("o76"), "ô");
    assert_eq!(keys("o67"), "ơ");
    // Free position, tone preserved: chặn + 6 → chận, chận + 8 → chặn.
    assert_eq!(keys("cha8n56"), "chận");
    assert_eq!(keys("cha6n58"), "chặn");
    // Shapes the base letter does not have are still a no-op: `a` has no móc,
    // so `7` is ignored here (unchanged — the engine restores the raw run).
    assert_eq!(keys("a67"), "â");
    // Same digit twice is still the revert.
    assert_eq!(keys("a66"), "a6");
    assert_eq!(keys("a88"), "a8");
}

#[test]
fn vni_shape_syllables() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "uo7"), "uơ");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "uo73"), "uở");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "thuo73"),
        "thuở"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "thuo7ng2"),
        "thường"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "quo7i1"),
        "quới"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "u7o7"), "ươ");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "u7o7ng"),
        "ương"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "tru7o7ng"),
        "trương"
    );
}

#[test]
fn vni_reposition() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "hoa2"), "hòa");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "chao2"), "chào");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "thuy3"), "thủy");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "khoe3"), "khỏe");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "hoaf2"), "hoàf");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "tru7o7n2g"),
        "trường"
    );
    assert_eq!(
        crate::support::type_words(InputMethod::Vni, "xin1 chao2 ban5"),
        "xín chào bạn"
    );
    // Open diphthongs ia / ua — tone on first vowel.
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "mua1"), "múa");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "cua3"), "của");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "mia1"), "mía");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "lua5"), "lụa");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "bua2"), "bùa");
}

#[test]
fn vni_revert() {
    // Double modifier restores raw keystrokes: strip diacritic + append the key.
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a11"), "a1");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a66"), "a6");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a88"), "a8");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "d99"), "d9");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a611"), "â1");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "a12"), "à"); // different key → re-tone
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "hoa22"), "hoa2");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "uo77"), "uo7");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "tru7o7n2g2"),
        "trương2"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "phu11"), "phu1");

    let (text, kinds) = crate::support::type_keys_with_kinds(InputMethod::Vni, "a11");
    assert_eq!(text, "a1");
    assert_eq!(kinds.last(), Some(&TransformKind::Reverted));
}

#[test]
fn vni_complex_syllables() {
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "ngu71"), "ngứ");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "truo7ng"),
        "trương"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "ngu7o7i2"),
        "người"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "vie5t"), "việt");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "nghia4"),
        "nghĩa"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "phuo7ng"),
        "phương"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "thuo7ng"),
        "thương"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "tru7o7n2g"),
        "trường"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "ngu7o7c1"),
        "ngước"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "lien4"), "liễn");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "d9i5nh"),
        "định"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "nghie5ng"),
        "nghiệng"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "nuoc71"),
        "nước"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "khuye6n2"),
        "khuyền"
    );
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "hoan2"), "hoàn");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "nghie6ng"),
        "nghiêng"
    );
}

#[test]
fn vni_complex_syllable_step_kinds() {
    let result = apply("ng", '1', InputMethod::Vni, ToneStyle::Traditional);
    assert_eq!(result.kind, TransformKind::Ignored);
    assert_eq!(result.text, "ng");

    let (_, kinds) = crate::support::type_keys_with_kinds(InputMethod::Vni, "ngu71");
    assert_eq!(kinds.last(), Some(&TransformKind::Applied));
}

#[test]
fn vni_qu_gi_onset_tone_placement() {
    // The `u` of `qu` and the `i` of `gi` are part of the onset, not the nucleus,
    // so the tone skips them.
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "qua1"), "quá");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "quan2"), "quàn");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "gia1"), "giá");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "gium2"), "giùm");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "quy1"), "quý");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "quoc61"),
        "quốc"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "quyen62"),
        "quyền"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "giuong72"),
        "giường"
    );

    // `gi` releases its `i` as the nucleus when no other vowel follows.
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "gi2"), "gì");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "gin2"), "gìn");
    // `gi` + `e` still forms `ê` (the `i` belongs to the onset digraph).
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "giet1"), "giết");
}

#[test]
fn vni_tone_digit_before_the_nucleus_reaches_the_glide_onsets() {
    // The digit parks on the glide (`gí`, `qú`) and moves once the nucleus lands.
    for (keys, expected) in [
        ("gi1a", "giá"),
        ("gi2a", "già"),
        ("gi3a", "giả"),
        ("gi4a", "giã"),
        ("gi5a", "giạ"),
        ("gi1o", "gió"),
        ("gi1ao", "giáo"),
        ("gi1an", "gián"),
        ("qu1a", "quá"),
        ("qu2a", "quà"),
        ("qu1y", "quý"),
        ("qu1an", "quán"),
        // A shape arriving after the tone still takes it along.
        ("gi1e6ng", "giếng"),
        ("qu1oc6", "quốc"),
        ("qu2yen6", "quyền"),
    ] {
        assert_eq!(
            crate::support::type_keys(InputMethod::Vni, keys),
            expected,
            "{keys}"
        );
    }
}

#[test]
fn vni_shaped_vowel_takes_tone() {
    // A vowel carrying mũ/móc/trần receives the tone, wherever it sits.
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "lay61"), "lấy");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "to6i2"), "tồi");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "mo7i1"), "mới");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "nau61"), "nấu");
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "muo6n1"),
        "muốn"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "tuo6i3"),
        "tuổi"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "nhie6u2"),
        "nhiều"
    );
    assert_eq!(
        crate::support::type_keys(InputMethod::Vni, "xoan82"),
        "xoằn"
    );
}

#[test]
fn vni_shape_targets_receiving_vowel() {
    // 6/7/8 land on the vowel that can take the shape, not just the last vowel.
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "loi6"), "lôi");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "hoi6"), "hôi");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "muoi6"), "muôi");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "toi6"), "tôi");
}

#[test]
fn vni_validation() {
    let result = apply("ng", '1', InputMethod::Vni, ToneStyle::Traditional);
    assert_eq!(result.kind, TransformKind::Ignored);
    assert_eq!(result.text, "ng");

    let result = apply("text", '1', InputMethod::Vni, ToneStyle::Traditional);
    assert_eq!(result.kind, TransformKind::Pending);
    assert_eq!(result.text, "text1");

    assert_eq!(crate::support::type_keys(InputMethod::Vni, "ma1"), "má");

    let result = apply("mix", '1', InputMethod::Vni, ToneStyle::Traditional);
    assert_eq!(result.kind, TransformKind::Applied);
    assert_eq!(result.text, "míx");

    let result = apply("zt", '1', InputMethod::Vni, ToneStyle::Traditional);
    assert_eq!(result.kind, TransformKind::Pending);
    assert_eq!(result.text, "zt1");
}
