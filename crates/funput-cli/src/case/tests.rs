use funput_core::charset::document;
use funput_core::textcase::Options;

use super::args::TransformArg;
use super::*;

/// Everything `run` does between reading and writing, with the I/O left out — the
/// same shape as `convert`'s tests, and for the same reason: the interesting part is
/// the decision, not the file handle.
fn pipeline(
    bytes: &[u8],
    transforms: &[TransformArg],
    options: Options,
) -> Result<String, CliError> {
    let document = document::read(bytes.to_vec()).expect("well-formed fixture");
    let text = readable(document)?;
    Ok(transforms
        .iter()
        .fold(text, |text, &arg| apply(&text, arg.into(), options)))
}

fn run_one(bytes: &[u8], transform: TransformArg) -> String {
    pipeline(bytes, &[transform], Options::default()).expect("fixture is Unicode")
}

#[test]
fn one_transform_end_to_end() {
    assert_eq!(
        run_one("Tiếng Việt rất đẹp".as_bytes(), TransformArg::NoDiacritics),
        "Tieng Viet rat dep"
    );
    assert_eq!(
        run_one("bàn phím tiếng Việt".as_bytes(), TransformArg::Title),
        "Bàn Phím Tiếng Việt"
    );
}

/// The reason stacking is offered at all, and the reason the order is the typed one.
#[test]
fn transforms_stack_in_the_order_given() {
    let shouting = "GỬI VỀ TP. HCM".as_bytes();
    let options = Options::default();

    let lower_then_title = [TransformArg::Lower, TransformArg::Title];
    assert_eq!(
        pipeline(shouting, &lower_then_title, options).unwrap(),
        "Gửi Về Tp. Hcm"
    );

    let title_then_lower = [TransformArg::Title, TransformArg::Lower];
    assert_eq!(
        pipeline(shouting, &title_then_lower, options).unwrap(),
        "gửi về tp. hcm"
    );
}

#[test]
fn keep_d_leaves_the_stroke_alone() {
    let options = Options {
        d_to_ascii: false,
        ..Options::default()
    };
    let kept = pipeline("đẹp".as_bytes(), &[TransformArg::NoDiacritics], options).unwrap();
    assert_eq!(kept, "đep");
}

#[test]
fn flatten_caps_stops_title_case_protecting_a_shout() {
    let options = Options {
        keep_all_caps: false,
        ..Options::default()
    };
    let flattened = pipeline("gửi về TP. HCM".as_bytes(), &[TransformArg::Title], options).unwrap();
    assert_eq!(flattened, "Gửi Về Tp. Hcm");
}

/// A `.VnTime` document — the fixture `convert`'s tests use, which converts cleanly
/// there and must be refused here.
#[test]
fn a_legacy_document_is_refused_by_name() {
    let err = pipeline(
        b"vi\xD6t nam h\xB5 n\xE9i",
        &[TransformArg::Upper],
        Options::default(),
    )
    .expect_err("TCVN3 is not Unicode");
    let CliError::Msg(message) = err else {
        panic!("expected a message, not an io error");
    };
    assert!(message.contains("tcvn3"), "{message}");
    assert!(message.contains("funput convert"), "{message}");
}

#[test]
fn bytes_that_are_not_text_are_refused_rather_than_guessed_at() {
    let err = pipeline(
        &[0xFF, 0x00, 0xFE],
        &[TransformArg::Upper],
        Options::default(),
    )
    .expect_err("not text");
    assert!(matches!(err, CliError::Msg(_)));
}

/// Reading goes through `charset::document`, so a byte-order mark and UTF-16 are
/// already handled. Pinned here so nobody simplifies it into `String::from_utf8`.
#[test]
fn a_utf16_file_with_a_mark_is_read_not_refused() {
    let mut bytes = vec![0xFF, 0xFE];
    for unit in "Tiếng Việt".encode_utf16() {
        bytes.extend_from_slice(&unit.to_le_bytes());
    }
    assert_eq!(
        pipeline(&bytes, &[TransformArg::NoDiacritics], Options::default()).unwrap(),
        "Tieng Viet"
    );
}
