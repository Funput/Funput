use super::*;

/// Convert a Rust string to the UTF-32 the ABI speaks.
fn utf32(text: &str) -> Vec<u32> {
    text.chars().map(|c| c as u32).collect()
}

fn read(out: &[u32], len: usize) -> String {
    out[..len]
        .iter()
        .filter_map(|&c| char::from_u32(c))
        .collect()
}

/// Call `funput_charset_convert` the way a host should: size, allocate, convert.
fn convert(text: &str, from: usize, to: usize) -> (String, FunputConversion) {
    let src = utf32(text);
    let sizing = unsafe {
        funput_charset_convert(src.as_ptr(), src.len(), from, to, std::ptr::null_mut(), 0)
    };
    let mut out = vec![0u32; sizing.len];
    let done = unsafe {
        funput_charset_convert(
            src.as_ptr(),
            src.len(),
            from,
            to,
            out.as_mut_ptr(),
            out.len(),
        )
    };
    assert_eq!(sizing, done, "the sizing call must predict the real one");
    (read(&out, done.len), done)
}

fn index_of(name: &str) -> usize {
    (0..funput_charset_count())
        .find(|&i| charset_at(i).is_some_and(|c| c.name() == name))
        .expect("charset not in the list")
}

#[test]
fn the_list_is_the_one_core_publishes() {
    assert_eq!(funput_charset_count(), funput_core::charset::ALL.len());
    assert!(funput_charset_count() > 0);
}

/// The point of exporting names at all: a host builds its menu without knowing a
/// single charset, and a charset added to core turns up in it.
#[test]
fn every_index_names_a_charset_and_nothing_past_the_end_does() {
    for index in 0..funput_charset_count() {
        let len = unsafe { funput_charset_name(index, std::ptr::null_mut(), 0) };
        assert!(len > 0, "charset {index} has no name");

        let mut out = vec![0u32; len];
        assert_eq!(
            unsafe { funput_charset_name(index, out.as_mut_ptr(), out.len()) },
            len
        );
        assert!(!read(&out, len).is_empty());
    }
    assert_eq!(
        unsafe { funput_charset_name(funput_charset_count(), std::ptr::null_mut(), 0) },
        0
    );
}

/// All-or-nothing, not truncation: a buffer one short is left untouched, so a host
/// that ignores the returned length reads nothing rather than half a document.
#[test]
fn a_buffer_that_is_one_short_is_not_written_to() {
    let src = utf32("Việt Nam");
    let unicode = index_of("Unicode dựng sẵn");
    let sizing = unsafe {
        funput_charset_convert(
            src.as_ptr(),
            src.len(),
            unicode,
            unicode,
            std::ptr::null_mut(),
            0,
        )
    };

    let mut out = vec![0u32; sizing.len - 1];
    let short = unsafe {
        funput_charset_convert(
            src.as_ptr(),
            src.len(),
            unicode,
            unicode,
            out.as_mut_ptr(),
            out.len(),
        )
    };
    assert_eq!(
        short.len, sizing.len,
        "the required length is still reported"
    );
    assert!(
        out.iter().all(|&c| c == 0),
        "nothing should have been written"
    );
}

#[test]
fn text_survives_a_trip_out_to_every_charset_and_back() {
    let unicode = index_of("Unicode dựng sẵn");
    for target in 0..funput_charset_count() {
        let (there, _) = convert("Việt Nam", unicode, target);
        let (back, _) = convert(&there, target, unicode);
        assert_eq!(back, "Việt Nam", "charset {target}");
    }
}

/// Pins the sizing advice in the doc: a conversion can be *longer* than its input,
/// so `cap = text_len` is not a safe guess.
#[test]
fn converting_to_vni_produces_more_characters_than_it_was_given() {
    let (text, out) = convert(
        "Việt",
        index_of("Unicode dựng sẵn"),
        index_of("VNI-Windows"),
    );
    assert!(out.len > "Việt".chars().count(), "{text:?}");
    assert_eq!(out.unmapped, 0);
}

/// TCVN3 has no code for an uppercase toned vowel, and the count says so on the
/// sizing call — before the host has allocated anything.
#[test]
fn what_cannot_be_represented_is_counted_before_any_buffer_exists() {
    let src = utf32("Ề");
    let out = unsafe {
        funput_charset_convert(
            src.as_ptr(),
            src.len(),
            index_of("Unicode dựng sẵn"),
            index_of("TCVN3 (ABC)"),
            std::ptr::null_mut(),
            0,
        )
    };
    assert_eq!(out.unmapped, 1);
    assert!(
        out.len > 0,
        "a character is never dropped, only spelled badly"
    );
}

#[test]
fn an_index_past_the_end_converts_nothing_rather_than_guessing() {
    let src = utf32("Việt Nam");
    let past = funput_charset_count();
    let out = unsafe {
        funput_charset_convert(src.as_ptr(), src.len(), past, 0, std::ptr::null_mut(), 0)
    };
    assert_eq!(out, FunputConversion::default());
}

#[test]
fn null_text_is_an_empty_conversion_rather_than_a_crash() {
    let out = unsafe { funput_charset_convert(std::ptr::null(), 7, 0, 0, std::ptr::null_mut(), 0) };
    assert_eq!(out, FunputConversion::default());
    assert_eq!(
        unsafe { funput_charset_detect(std::ptr::null(), 7) },
        FUNPUT_CHARSET_UNKNOWN
    );
}

/// The index detection hands back is one `funput_charset_convert` accepts — the
/// whole reason both speak in indices rather than names.
#[test]
fn what_detection_returns_can_be_fed_straight_back_in() {
    let unicode = index_of("Unicode dựng sẵn");
    let (tcvn3_text, _) = convert("việt nam hà nội", unicode, index_of("TCVN3 (ABC)"));

    let src = utf32(&tcvn3_text);
    let detected = unsafe { funput_charset_detect(src.as_ptr(), src.len()) };
    assert_eq!(detected, index_of("TCVN3 (ABC)") as i32);

    let (back, _) = convert(&tcvn3_text, detected as usize, unicode);
    assert_eq!(back, "việt nam hà nội");
}

/// Declining is the honest answer for text that reads the same under every
/// candidate, and a host must not treat it as a failure.
#[test]
fn ascii_is_declined_rather_than_guessed_at() {
    let src = utf32("the quick brown fox");
    assert_eq!(
        unsafe { funput_charset_detect(src.as_ptr(), src.len()) },
        FUNPUT_CHARSET_UNKNOWN
    );
}
