//! Personal names written with an older spelling of a native onset (`Dzũng`,
//! `Hồ Dzếnh`, `Dzoãn`) type in both methods with no setting, and English words
//! that begin the same way still restore.

use funput_core::InputMethod;

use crate::support::app_text;

fn telex(keys: &str) -> String {
    app_text(InputMethod::Telex, keys)
}

fn vni(keys: &str) -> String {
    app_text(InputMethod::Vni, keys)
}

#[test]
fn dz_names_type_in_both_methods() {
    let names = "Dzũng Hồ Dzếnh Nguyễn Dzoãn dzui DZŨNG ";
    assert_eq!(
        telex("Dzungx Hoof Dzeenhs Nguyeenx Dzoanx dzui DZUNGX "),
        names
    );
    assert_eq!(
        vni("Dzung4 Ho62 Dze6nh1 Nguye6n4 Dzoan4 dzui DZUNG4 "),
        names
    );
}

#[test]
fn telex_z_still_clears_the_mark_after_dz() {
    // In `dz` the `z` comes before any mark, so it is a letter; a later `z` is
    // the clear key as ever.
    assert_eq!(telex("dzungxz "), "dzung ");
}

#[test]
fn english_after_dz_still_restores() {
    for word in ["dzungar ", "Dzungar "] {
        assert_eq!(telex(word), word);
        assert_eq!(vni(word), word);
    }
}
