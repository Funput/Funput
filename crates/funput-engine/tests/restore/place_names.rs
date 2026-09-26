//! Tây Nguyên place and ethnic names must survive English restore, in both
//! methods — without letting English words through in their place.

use funput_core::InputMethod;

use crate::support::app_text;

fn telex(keys: &str) -> String {
    app_text(InputMethod::Telex, keys)
}

fn vni(keys: &str) -> String {
    app_text(InputMethod::Vni, keys)
}

#[test]
fn cluster_onsets_and_final_k_type_in_both_methods() {
    assert_eq!(
        telex("ddawks lawks kroong buks kpaw xtieeng hree rcawm "),
        "đắk lắk krông búk kpă xtiêng hrê rcăm "
    );
    assert_eq!(
        vni("d9a8k1 la8k1 kro6ng bu1k kpa8 xtie6ng hre6 rca8m "),
        "đắk lắk krông búk kpă xtiêng hrê rcăm "
    );
}

#[test]
fn m_drak_strikes_its_d_on_the_spot() {
    // M'Đrắk: the apostrophe ends a word, so `Đrắk` is typed on its own.
    for keys in ["Ddrawsk ", "Ddrawks "] {
        assert_eq!(telex(keys), "Đrắk ", "{keys}");
    }
    for keys in ["D9ra8k1 ", "D9ra81k "] {
        assert_eq!(vni(keys), "Đrắk ", "{keys}");
    }
}

#[test]
fn vni_keeps_the_finals_telex_cannot_afford() {
    // Chư Păh, Chư Pưh, Đạ Tẻh, Ea Nuôl, Blơr — digits first or last.
    for (keys, name) in [
        ("Pa8h ", "Păh "),
        ("Pah8 ", "Păh "),
        ("Pu7h ", "Pưh "),
        ("Te3h ", "Tẻh "),
        ("Teh3 ", "Tẻh "),
        ("Nuo6l ", "Nuôl "),
        ("Blo7r ", "Blơr "),
    ] {
        assert_eq!(vni(keys), name, "{keys}");
    }
}

#[test]
fn telex_types_colliding_names_through_the_double_key() {
    // `r` is the hỏi key and `a…a` the circumflex, so Ea Kar, Cư M'gar, Krông Ana
    // and Blơr are typed by doubling the key, which the boundary then keeps.
    for (keys, name) in [
        ("Karr ", "Kar "),
        ("garr ", "gar "),
        ("Anaa ", "Ana "),
        ("Blowrr ", "Blơr "),
    ] {
        assert_eq!(telex(keys), name, "{keys}");
    }
}

#[test]
fn english_still_restores_in_telex() {
    // What the name spellings would have let through: `s` + `h` is English `sh`,
    // `kn`/`sl` begin English words, and a late `d` never strikes a `dr` cluster.
    for word in [
        "cash ", "bush ", "aha ", "coho ", "know ", "knee ", "slow ", "cool ", "droid ", "dried ",
        "druid ",
    ] {
        assert_eq!(telex(word), word);
    }
}

#[test]
fn vni_digits_glued_to_words_still_restore() {
    for word in ["e2e ", "u23vn ", "a4paper ", "a1b ", "covid19 ", "mix4 "] {
        assert_eq!(vni(word), word);
    }
}
