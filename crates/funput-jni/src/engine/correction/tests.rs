//! What the IME decodes, tested without a JVM: only the JNI marshalling needs one,
//! and none of the decisions live there.

use funput_engine::{Action, ImeResult};

use super::*;

fn send(backspace: usize, output: &str) -> ImeResult {
    ImeResult {
        action: Action::Send,
        backspace,
        output: output.to_owned(),
    }
}

#[test]
fn an_edit_encodes_as_its_backspace_count_then_its_text() {
    let encoded = encode_edit(Some(send(10, "đường ")));
    assert_eq!(encoded[0], 10);
    let text: String = encoded[1..]
        .iter()
        .filter_map(|&c| char::from_u32(c as u32))
        .collect();
    assert_eq!(text, "đường ");
}

#[test]
fn a_no_op_encodes_as_nothing_at_all() {
    // An empty array is how the IME is told to leave the document alone; a `[0]`
    // would read as a real edit that deletes nothing and inserts nothing.
    assert!(encode_edit(None).is_empty());
    assert!(
        encode_edit(Some(ImeResult {
            action: Action::None,
            backspace: 0,
            output: String::new(),
        }))
        .is_empty()
    );
}

#[test]
fn a_code_point_java_cannot_have_typed_is_not_a_key() {
    // Java hands codepoints over as a signed int, so -1 is how the IME says "no
    // neighbour here" and must never decode to a character.
    assert_eq!(to_char('h' as jint), Some('h'));
    assert_eq!(to_char(-1), None);
    assert_eq!(to_char(0x11_0000), None, "past the last plane");
    assert_eq!(to_char(0xD800), None, "a surrogate is not a scalar");
}
