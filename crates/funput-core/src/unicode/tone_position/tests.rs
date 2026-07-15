use super::*;

fn char_at(buffer: &str, index: usize) -> char {
    buffer.chars().nth(index).expect("char at index")
}

/// `tone_vowel_index` with the default (traditional) style.
fn trad(buffer: &str) -> usize {
    tone_vowel_index(buffer, ToneStyle::Traditional).unwrap()
}

/// `tone_vowel_index` with the modern ("kiểu mới") style.
fn modern(buffer: &str) -> usize {
    tone_vowel_index(buffer, ToneStyle::Modern).unwrap()
}

#[test]
fn tone_vowel_index_open_diphthong_first_vowel() {
    // Traditional rule: open 2-vowel cluster → tone on the first vowel.
    assert_eq!(char_at("hoa", trad("hoa")), 'o'); // hòa
    assert_eq!(char_at("chao", trad("chao")), 'a'); // chào
    assert_eq!(char_at("hoe", trad("hoe")), 'o'); // hòe
}

#[test]
fn tone_vowel_index_uy_open_is_first_vowel() {
    // Open `uy` → tone on `u` (ùy/úy), traditional style.
    assert_eq!(char_at("thuy", trad("thuy")), 'u');
}

#[test]
fn tone_vowel_index_oa_with_coda_is_second_vowel() {
    // 2 vowels + final consonant → second vowel: hoàn, toán.
    assert_eq!(char_at("hoan", trad("hoan")), 'a');
    assert_eq!(char_at("toan", trad("toan")), 'a');
}

#[test]
fn tone_vowel_index_single_vowel() {
    assert_eq!(char_at("ma", trad("ma")), 'a');
    assert_eq!(char_at("ho", trad("ho")), 'o');
}

#[test]
fn tone_vowel_index_uo_horn_cluster() {
    assert_eq!(char_at("trương", trad("trương")), 'ơ');
    assert_eq!(char_at("thuơ", trad("thuơ")), 'ơ');
}

#[test]
fn tone_vowel_index_open_diphthongs_ia_ua() {
    assert_eq!(char_at("mia", trad("mia")), 'i');
    assert_eq!(char_at("mua", trad("mua")), 'u');
    assert_eq!(char_at("cua", trad("cua")), 'u');
    assert_eq!(char_at("lua", trad("lua")), 'u');
}

#[test]
fn tone_vowel_index_uoi_cluster() {
    assert_eq!(char_at("ngươi", trad("ngươi")), 'ơ');
}

#[test]
fn tone_vowel_index_plain_triphthong_is_middle() {
    // Plain triphthongs take the tone on the middle vowel, not the last.
    assert_eq!(char_at("ngoai", trad("ngoai")), 'a'); // ngoài
    assert_eq!(char_at("xoay", trad("xoay")), 'a'); // xoáy
    assert_eq!(char_at("khuyu", trad("khuyu")), 'y'); // khuỷu
}

#[test]
fn tone_vowel_index_modern_moves_oa_oe_uy_to_second() {
    // "Kiểu mới": open oa/oe/uy → tone on the second (main) vowel.
    assert_eq!(char_at("hoa", modern("hoa")), 'a'); // hoà
    assert_eq!(char_at("hoe", modern("hoe")), 'e'); // hoè
    assert_eq!(char_at("thuy", modern("thuy")), 'y'); // thuý
}

#[test]
fn tone_vowel_index_modern_leaves_other_clusters_unchanged() {
    // Only oa/oe/uy differ — everything else matches the traditional rule.
    assert_eq!(char_at("mia", modern("mia")), 'i'); // mía
    assert_eq!(char_at("mua", modern("mua")), 'u'); // múa
    assert_eq!(char_at("chao", modern("chao")), 'a'); // chào
    assert_eq!(char_at("hoan", modern("hoan")), 'a'); // hoàn (coda)
    assert_eq!(char_at("ngoai", modern("ngoai")), 'a'); // ngoài (triphthong)
    assert_eq!(char_at("trương", modern("trương")), 'ơ'); // shaped vowel wins
}

#[test]
fn tone_target_vowel_ie_uses_circumflex_e() {
    assert_eq!(tone_target_vowel("viet", 2), Some('ê'));
    assert_eq!(tone_target_vowel("lien", 2), Some('ê'));
}

#[test]
fn reposition_moves_tone_to_first_vowel_of_open_diphthong() {
    // Traditional: an open `oa` takes the tone on `o`, so `hoà` → `hòa`.
    assert_eq!(
        reposition_existing_tone("hoà", ToneStyle::Traditional).as_deref(),
        Some("hòa")
    );
}

#[test]
fn reposition_modern_moves_tone_to_second_vowel() {
    // Modern: an open `oa` takes the tone on `a`, so `hòa` → `hoà`.
    assert_eq!(
        reposition_existing_tone("hòa", ToneStyle::Modern).as_deref(),
        Some("hoà")
    );
    // Already correct for the style → no change.
    assert_eq!(reposition_existing_tone("hoà", ToneStyle::Modern), None);
}
