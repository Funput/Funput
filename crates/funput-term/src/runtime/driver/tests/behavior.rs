use super::*;

#[test]
fn telex_word_emits_backspace_and_unicode() {
    let mut expected = b"a".to_vec();
    expected.push(0x7f);
    expected.extend_from_slice("á".as_bytes());
    assert_eq!(compose(InputMethod::Telex, b"as"), expected);
}

#[test]
fn enter_triggers_english_restore() {
    assert_eq!(
        reconstruct(&compose(InputMethod::Telex, b"text\r")),
        "text\r"
    );
}

#[test]
fn backspace_corrects_mid_composition() {
    assert_eq!(
        reconstruct(&compose(InputMethod::Telex, b"Phua\x7fs")),
        "Phú"
    );
}

#[test]
fn double_modifier_revert_sends_no_extra_char() {
    assert_eq!(reconstruct(&compose(InputMethod::Telex, b"mixx")), "mix");
}

#[test]
fn enter_keeps_valid_vietnamese() {
    assert_eq!(reconstruct(&compose(InputMethod::Telex, b"mas\r")), "má\r");
}

#[test]
fn bracketed_paste_is_forwarded_verbatim() {
    let out = compose(InputMethod::Telex, b"\x1b[200~as\x1b[201~");
    assert!(out.starts_with(b"\x1b[200~"));
    assert!(out.ends_with(b"\x1b[201~"));
    assert_eq!(reconstruct(&out), "\x1b[200~as\x1b[201~");
}

#[test]
fn composition_resumes_after_paste() {
    let out = compose(InputMethod::Telex, b"as\x1b[200~as\x1b[201~as");
    assert_eq!(reconstruct(&out), "á\x1b[200~as\x1b[201~á");
}
