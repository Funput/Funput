//! The undo calls the IME makes, run against a real engine in the registry. Only the
//! JNI marshalling needs a JVM; none of the decisions live there.

use jni::sys::jint;

use super::super::correction::encode_edit;
use super::*;

/// A handle whose engine has just expanded `vn` to `Việt Nam` on a space.
fn expanded() -> jlong {
    let handle = registry::create();
    registry::with_mut(handle, |engine| {
        engine.add_shortcut("vn", "Việt Nam");
        for key in "vn ".chars() {
            engine.process_char(key);
        }
    });
    handle
}

fn decode(encoded: &[jint]) -> (jint, String) {
    let text = encoded[1..]
        .iter()
        .filter_map(|&c| char::from_u32(c as u32))
        .collect();
    (encoded[0], text)
}

#[test]
fn the_undo_reads_back_through_the_registry_then_spends_itself() {
    let handle = expanded();
    assert_eq!(
        registry::with_mut(handle, |engine| engine.has_shortcut_undo()),
        Some(true)
    );
    assert_eq!(
        shortcut_string(handle, |engine| engine.shortcut_undo_text()),
        "vn"
    );
    assert_eq!(
        shortcut_string(handle, |engine| engine.shortcut_undo_written()),
        "Việt Nam "
    );

    let edit = registry::with_mut(handle, |engine| engine.undo_shortcut()).flatten();
    let (backspace, text) = decode(&encode_edit(edit));
    assert_eq!((backspace, text.as_str()), (9, "vn "));

    assert_eq!(
        registry::with_mut(handle, |engine| engine.has_shortcut_undo()),
        Some(false)
    );
    assert_eq!(
        shortcut_string(handle, |engine| engine.shortcut_undo_text()),
        ""
    );
    let again = registry::with_mut(handle, |engine| engine.undo_shortcut()).flatten();
    assert!(encode_edit(again).is_empty(), "nothing armed is no edit");
    registry::destroy(handle);
}

#[test]
fn dismissing_drops_the_undo() {
    let handle = expanded();
    registry::with_mut(handle, |engine| engine.dismiss_shortcut_undo());

    assert_eq!(
        registry::with_mut(handle, |engine| engine.has_shortcut_undo()),
        Some(false)
    );
    let edit = registry::with_mut(handle, |engine| engine.undo_shortcut()).flatten();
    assert!(encode_edit(edit).is_empty());
    registry::destroy(handle);
}

#[test]
fn an_unknown_or_destroyed_handle_answers_with_nothing() {
    let handle = expanded();
    registry::destroy(handle);

    assert!(registry::with_mut(handle, |engine| engine.has_shortcut_undo()).is_none());
    assert_eq!(
        shortcut_string(handle, |engine| engine.shortcut_undo_text()),
        ""
    );
    assert_eq!(
        shortcut_string(handle, |engine| engine.shortcut_undo_written()),
        ""
    );
    assert!(registry::with_mut(handle, |engine| engine.undo_shortcut()).is_none());
}
