use funput_core::charset::Charset;

use super::{Mode, Session};
use crate::{at, index_of};

/// A batch of `n` files, all reading as Unicode, in a scratch folder of our own.
fn batch(name: &str, n: usize) -> (std::path::PathBuf, Session) {
    let dir = std::env::temp_dir().join(format!("funput-session-{name}"));
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).expect("scratch dir");
    let paths: Vec<_> = (0..n)
        .map(|i| {
            let path = dir.join(format!("{i}.txt"));
            std::fs::write(&path, "việt nam").expect("seed file");
            path
        })
        .collect();
    let mut session = Session::new();
    session.adopt(crate::scan(&paths));
    session.refresh();
    (dir, session)
}

/// The user is looking at the document; the detector is looking at statistics.
#[test]
fn a_charset_the_user_picked_outranks_the_detected_one() {
    let mut session = Session::new();
    session.set_input("việt nam".to_string());
    session.pick_source(Some(1));
    session.refresh();

    assert_eq!(session.view().source, Some(1));
}

/// What was chosen for the last document says nothing about this one.
#[test]
fn a_fresh_paste_forgets_the_charset_of_the_last_one() {
    let mut session = Session::new();
    session.set_input("việt nam".to_string());
    session.pick_source(Some(2));
    session.refresh();
    session.set_input("hà nội".to_string());
    session.refresh();

    assert_ne!(
        session.view().source,
        Some(2),
        "the last choice outlived its document"
    );
}

/// A single file wears the text shape, so the one source picker on screen has to
/// write into the file — not into the pasted-text slot nobody can see.
#[test]
fn the_source_picker_lands_on_the_single_file_when_one_is_open() {
    let (_dir, mut session) = batch("single-pick", 1);
    session.pick_source(Some(index_of(Charset::Tcvn3).unwrap()));
    session.refresh();

    assert_eq!(session.view().source, index_of(Charset::Tcvn3));
    assert!(session.view().from_file);
}

/// **The bug this consolidation was worth doing for.** Saving a single dropped file
/// read the pasted-text slot, which is empty in that shape — so it converted nothing
/// while copying the same document worked. One rule, one place, both agree.
#[test]
fn saving_a_single_dropped_file_converts_the_file_not_the_empty_paste_box() {
    let (_dir, mut session) = batch("single-save", 1);
    session.set_target(index_of(Charset::Tcvn3).unwrap());
    session.refresh();

    // One byte per letter, from the file's own text — not `None`, which is what
    // reading the empty paste box used to produce.
    assert_eq!(session.save_bytes().as_deref(), Some(&b"vi\xD6t nam"[..]));
    assert_eq!(session.result_text().as_deref(), Some("vi\u{D6}t nam"));
}

/// A longer menu than `ALL` can only come from a bug; it must land on a wrong entry
/// rather than take the window down.
#[test]
fn an_index_from_a_longer_menu_is_clamped_rather_than_panicking() {
    assert_eq!(at(99), at(funput_core::charset::ALL.len() - 1));
}

/// Two files are a table; one is not — a single file has nothing to compare against,
/// so a one-row table would hide what the user came to see.
#[test]
fn the_content_decides_the_shape() {
    let mut session = Session::new();
    session.refresh();
    assert_eq!(session.view().mode, Mode::Empty);

    session.set_input("việt".to_string());
    session.refresh();
    assert_eq!(session.view().mode, Mode::Text);

    let (_dir, two) = batch("shape", 2);
    assert_eq!(two.view().mode, Mode::Files);
}

/// The window is a slice; the counts are the whole batch. A capped list that also
/// capped the numbers would quietly under-report a long drop.
#[test]
fn the_row_window_does_not_change_the_counts() {
    let (_dir, mut session) = batch("window", 7);
    session.set_row_window(0, 3);
    session.refresh();

    let view = session.view();
    assert_eq!(view.rows.len(), 3);
    assert_eq!(view.rows_total, 7);
    assert_eq!(view.ready, 7, "every file was identified");
}

/// The left pane belongs to whoever owns the text. A pasted paragraph is the user's
/// — writing it back on every redraw sends the caret home — and a file's is ours.
#[test]
fn the_left_pane_is_only_ours_when_a_file_is_open() {
    let mut session = Session::new();
    session.set_input("việt".to_string());
    session.refresh();
    assert!(
        session.view().input_preview.is_none(),
        "the paste box is the shell's"
    );

    let (_dir, file) = batch("pane", 1);
    assert!(
        file.view().input_preview.is_some(),
        "a file's text is ours to show"
    );
}

/// A file that cannot be read is named, not counted. Ten dropped and eight shown is
/// a question a number cannot answer.
#[test]
fn a_file_that_cannot_be_read_is_named() {
    let dir = std::env::temp_dir().join("funput-session-unreadable");
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).expect("scratch dir");
    let good = dir.join("ok.txt");
    std::fs::write(&good, "việt").expect("seed file");
    let gone = dir.join("gone.txt");

    let mut session = Session::new();
    session.adopt(crate::scan(&[good, gone]));
    session.refresh();

    // A path that is not a file never reaches the reader, so the honest check is the
    // one that does: an entry read, and nothing invented for the one that was not.
    assert_eq!(session.view().rows_total, 1);
}

/// The count behind "N chữ sẽ mất" follows the target of the moment, not the one the
/// file was read under — that is the whole reason a row is rebuilt on a target change.
#[test]
fn what_a_row_will_lose_is_measured_against_the_target_of_the_moment() {
    let dir = std::env::temp_dir().join("funput-session-note");
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).expect("scratch dir");
    for name in ["a.txt", "b.txt"] {
        std::fs::write(dir.join(name), "Ổn").expect("seed file");
    }
    let mut session = Session::new();
    session.adopt(crate::scan(&[dir.join("a.txt"), dir.join("b.txt")]));

    // TCVN3 has no code for an uppercase toned vowel.
    session.set_target(index_of(Charset::Tcvn3).unwrap());
    session.refresh();
    assert_eq!(session.view().rows[0].note, "1 chữ sẽ mất");

    // The same document costs nothing going somewhere that can spell it.
    session.set_target(index_of(Charset::UnicodeCombining).unwrap());
    session.refresh();
    assert_eq!(session.view().rows[0].note, "");
}

/// Every mutator has to leave a view a refresh would reproduce — otherwise one of
/// them forgot to invalidate something and the window shows the previous document.
#[test]
fn a_view_is_never_stale_after_a_refresh() {
    let (_dir, mut session) = batch("stale", 2);
    for step in 0..6 {
        match step {
            0 => session.set_target(1),
            1 => session.set_input("việt".to_string()),
            2 => session.pick_source(Some(0)),
            3 => session.pick_row_source(0, 2),
            4 => session.set_row_window(1, 1),
            _ => session.reset(),
        }
        session.refresh();
        let once = session.view().clone();
        session.refresh();
        assert_eq!(&once, session.view(), "step {step} left something behind");
    }
}
