use super::*;

/// (onset, nucleus, coda, invalid_onset) with the iterators materialized.
fn parts(buffer: &str) -> (String, String, String, bool) {
    let p = parse_syllable(buffer);
    (
        p.onset.into(),
        p.nucleus_chars().collect(),
        p.coda_chars().collect(),
        p.invalid_onset,
    )
}

fn ok(onset: &str, nucleus: &str, coda: &str) -> (String, String, String, bool) {
    (onset.into(), nucleus.into(), coda.into(), false)
}

#[test]
fn parse_syllable_cases() {
    assert_eq!(parts("tr"), ok("tr", "", ""));
    assert_eq!(parts("ng"), ok("ng", "", ""));
    assert_eq!(parts("ma"), ok("m", "a", ""));
    assert_eq!(parts("text"), ok("t", "e", "xt"));
    assert_eq!(parts("mix"), ok("m", "i", "x"));
    assert_eq!(parts("trung"), ok("tr", "u", "ng"));
    assert_eq!(parts("đ"), ok("đ", "", ""));
    // `gi` releases its `i` as the nucleus when no vowel follows.
    assert_eq!(parts("gi"), ok("g", "i", ""));
    assert_eq!(parts("gia"), ok("gi", "a", ""));
    // No valid onset → the leading consonants flag the chunk.
    assert_eq!(parts("zt"), ("".into(), "".into(), "zt".into(), true));
    assert_eq!(parts(""), ok("", "", ""));
}

#[test]
fn onset_survives_a_tone_parked_on_the_glide() {
    // Mid-composition transients: the tone key arrived before the nucleus. These
    // must keep parsing as `gi`/`qu` onsets, or the rhyme reads as `io`/`ua` and
    // the engine restores the raw keystrokes (`giso`, `qusa`).
    assert_eq!(parts("gío"), ok("gí", "o", ""));
    assert_eq!(parts("gía"), ok("gí", "a", ""));
    assert_eq!(parts("qúa"), ok("qú", "a", ""));
    assert_eq!(parts("qúy"), ok("qú", "y", ""));
    // `qu` keeps its glide even with nothing after it — there is no `q` + `u`
    // nucleus syllable to fall back to.
    assert_eq!(parts("qú"), ok("qú", "", ""));
    // …while `gi` still releases a lone toned `i` as the nucleus: `gì`, `gìn`.
    assert_eq!(parts("gì"), ok("g", "ì", ""));
    assert_eq!(parts("gìn"), ok("g", "ì", "n"));
}

#[test]
fn horned_u_is_not_a_qu_glide() {
    // `ư` is a different vowel, not a toned `u`, so `qư` forms no onset at all
    // (the bare `q` is what flags the chunk).
    assert!(parse_syllable("qư").invalid_onset);
    assert!(!is_valid_onset("qư"));
}
