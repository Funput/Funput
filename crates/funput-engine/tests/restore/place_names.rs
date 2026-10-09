//! Tây Nguyên place and ethnic names must survive English restore, in both
//! methods — without letting English words through in their place.

use funput_core::InputMethod;
use funput_engine::Engine;

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
    // Chư Păh, Chư Pưh, Đạ Tẻh, Ea Nuôl, Blơr: the final typed after a mark, or a
    // digit landing on a word that already carries one (`Pa8h1`, the `đ` of `Đêh`).
    for (keys, name) in [
        ("Pa8h ", "Păh "),
        ("Pu7h ", "Pưh "),
        ("Te3h ", "Tẻh "),
        ("Nuo6l ", "Nuôl "),
        ("Blo7r ", "Blơr "),
        ("Pa8h1 ", "Pắh "),
        ("D9eh6 ", "Đêh "),
    ] {
        assert_eq!(vni(keys), name, "{keys}");
    }
}

#[test]
fn vni_mark_last_on_a_bare_name_comes_back_through_flip() {
    // `Pah8` types exactly like `bar8`, so it restores; Flip recovers the name.
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Vni);
    for key in "Pah8".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "Pah8");
    engine.flip_composing();
    assert_eq!(engine.buffer(), "Păh");
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
    // Including words ending in a name final: `bar1` must not stay `bár`.
    for word in [
        "e2e ", "u23vn ", "a4paper ", "a1b ", "covid19 ", "mix4 ", "bar1 ", "cool2 ", "ver2 ",
        "tier1 ", "ah1 ",
    ] {
        assert_eq!(vni(word), word);
    }
}
