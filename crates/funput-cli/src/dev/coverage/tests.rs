use std::path::PathBuf;

use super::*;

fn sample_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../benchmarks/sample.txt")
}

#[test]
fn sample_round_trips_through_every_rust_profile() {
    let syllables = corpus::load_syllables(&sample_path()).expect("load sample corpus");
    assert_eq!(syllables.len(), 137);

    let mut full_shortcuts = 0;
    for syllable in syllables {
        assert!(round_trips(
            &syllable,
            InputMethod::Telex,
            InputMethod::Telex
        ));
        assert!(round_trips(
            &syllable,
            InputMethod::Telex,
            InputMethod::TelexAdvanced
        ));
        assert!(round_trips(
            &syllable,
            InputMethod::TelexAdvanced,
            InputMethod::TelexAdvanced
        ));
        assert!(round_trips(&syllable, InputMethod::Vni, InputMethod::Vni));

        let keys = encode(&syllable, InputMethod::TelexAdvanced);
        full_shortcuts += keys.matches(['w', 'W', '[', ']']).count();
    }
    assert!(
        full_shortcuts > 0,
        "sample must exercise Full Telex shortcuts"
    );
}
