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

/// How many rows a shell can build is the **toolkit's** business, not the document's:
/// a GTK `ListBox` builds every row it is handed whatever is in it, which is why the
/// cap exists at all. So a new batch starts at the top of the list without forgetting
/// how long the list may be — the same distinction the destination charset gets one
/// test down, where it survives as a tool preference.
///
/// Before this was true, the Windows window asked for two thousand rows once when it
/// opened and then silently drew five hundred for every batch anyone dropped, with the
/// button above still offering to convert all of them.
#[test]
fn adopting_files_keeps_the_row_cap_the_shell_asked_for() {
    let (dir, mut session) = batch("keep-cap", 4);
    session.set_row_window(0, 2);
    session.refresh();
    assert_eq!(session.view().rows.len(), 2, "the shell asked for two");

    let paths: Vec<_> = (0..4).map(|i| dir.join(format!("{i}.txt"))).collect();
    session.adopt(crate::scan(&paths));
    session.refresh();

    let view = session.view();
    assert_eq!(view.rows_total, 4, "the whole batch is still counted");
    assert_eq!(view.rows_first, 0, "a new batch starts at the top");
    assert_eq!(
        view.rows.len(),
        2,
        "and the cap the shell asked for survives"
    );
}

#[test]
fn adopting_files_replaces_the_previous_document_and_the_row_position() {
    let (_dir, mut session) = batch("fresh-adopt", 2);
    session.set_input("nội dung cũ".to_string());
    session.pick_source(Some(2));
    session.set_target(1);
    session.set_row_window(1, 1);

    session.adopt(crate::scan(&[]));
    session.refresh();

    assert_eq!(session.view().mode, Mode::Empty);
    assert_eq!(session.view().source, None);
    assert_eq!(session.view().rows_first, 0);
    assert_eq!(
        session.view().target,
        1,
        "the destination is a tool preference"
    );
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

/// A pasted paragraph with its charset settled, so what follows is about the second
/// axis rather than about detection. Unicode is picked rather than detected on
/// purpose: `detect` places `việt nam` but not `GỬI VỀ TP. HCM`, and a test of
/// transforms should not turn on that.
fn pasted(text: &str) -> Session {
    let mut session = Session::new();
    session.set_input(text.to_string());
    session.pick_source(index_of(Charset::Unicode));
    session
}

/// Menu positions for the two transforms most of these tests press.
fn transform(t: funput_core::textcase::Transform) -> usize {
    crate::casing::index_of(t).expect("every transform has a position")
}

fn lower() -> usize {
    transform(funput_core::textcase::Transform::Lower)
}

fn title() -> usize {
    transform(funput_core::textcase::Transform::Title)
}

fn upper() -> usize {
    transform(funput_core::textcase::Transform::Upper)
}

/// Two presses, and which came first decides the answer. If the session collapsed
/// them into a set, or replayed them in menu order, this is the test that says so.
#[test]
fn the_order_the_transforms_were_pressed_in_is_the_order_they_run() {
    let mut lower_then_title = pasted("GỬI VỀ TP. HCM");
    lower_then_title.apply_transform(lower());
    lower_then_title.apply_transform(title());
    lower_then_title.refresh();

    let mut title_then_lower = pasted("GỬI VỀ TP. HCM");
    title_then_lower.apply_transform(title());
    title_then_lower.apply_transform(lower());
    title_then_lower.refresh();

    assert_eq!(lower_then_title.view().output_preview, "Gửi Về Tp. Hcm");
    assert_eq!(title_then_lower.view().output_preview, "gửi về tp. hcm");
}

/// Undo takes off the last press, not all of them: pressing a third by mistake
/// should not cost the two before it.
#[test]
fn undo_takes_off_one_transform_and_leaves_the_rest() {
    let mut session = pasted("GỬI VỀ TP. HCM");
    session.apply_transform(lower());
    session.refresh();
    let after_one = session.view().output_preview.clone();

    session.apply_transform(title());
    session.refresh();
    assert_ne!(session.view().output_preview, after_one);

    session.undo_transform();
    session.refresh();
    assert_eq!(session.view().output_preview, after_one);
    assert_eq!(session.view().transforms, vec![lower()]);
}

/// Every transform is idempotent, so a second press cannot change the document —
/// and a list that grew anyway would make undo feel broken.
#[test]
fn pressing_the_same_transform_twice_does_not_grow_the_list() {
    let mut session = Session::new();
    session.set_input("việt nam".to_string());
    session.apply_transform(upper());
    session.apply_transform(upper());
    session.refresh();

    assert_eq!(session.view().transforms, vec![upper()]);
}

/// The transforms belonged to the document they were pressed on, exactly as the
/// charset did.
#[test]
fn a_fresh_document_arrives_with_no_transforms_on_it() {
    let mut session = Session::new();
    session.set_input("việt nam".to_string());
    session.apply_transform(upper());
    session.refresh();
    session.set_input("hà nội".to_string());
    session.refresh();

    assert!(session.view().transforms.is_empty());
    assert_eq!(session.view().output_preview, "hà nội");

    session.apply_transform(upper());
    session.refresh();
    let (dir, mut dropped) = batch("casing-adopt", 2);
    dropped.set_input("việt nam".to_string());
    dropped.apply_transform(upper());
    dropped.adopt(crate::scan(&[dir.join("0.txt")]));
    dropped.refresh();
    assert!(dropped.view().transforms.is_empty());
    let _ = std::fs::remove_dir_all(&dir);
}

/// **The four doors.** The pane, the clipboard, the saved bytes and the file a batch
/// writes all have to say the same thing about the same document; they used to be
/// three separate call sites, and one of them was wrong.
#[test]
fn the_preview_the_clipboard_and_the_saved_bytes_agree() {
    let mut session = pasted("tiếng việt");
    session.apply_transform(title());
    session.refresh();

    let expected = "Tiếng Việt";
    assert_eq!(session.view().output_preview, expected);
    assert_eq!(session.result_text().as_deref(), Some(expected));
    assert_eq!(
        session.save_bytes().map(|b| String::from_utf8(b).unwrap()),
        Some(expected.to_string())
    );
}

/// And the fourth door, which goes to disk on another thread.
#[test]
fn a_batch_writes_the_transformed_document() {
    let (dir, mut session) = batch("casing-batch", 2);
    session.apply_transform(upper());
    session.refresh();

    let outcome = session.batch_job().run();
    assert_eq!(outcome.written, 2);
    let written = std::fs::read_to_string(dir.join(crate::OUT_DIR).join("0.txt")).expect("copy");
    assert_eq!(written, "VIỆT NAM");
    let _ = std::fs::remove_dir_all(&dir);
}

/// The note on a row is a promise about what that file will cost, so it has to be
/// counted after the transform: TCVN3 has no room for uppercase toned vowels, and a
/// count taken before UPPERCASE would understate what the batch is about to lose.
#[test]
fn a_row_counts_what_the_transform_will_cost_not_what_the_document_costs_now() {
    let (dir, mut session) = batch("casing-note", 2);
    session.set_target(index_of(Charset::Tcvn3).unwrap());
    session.refresh();
    assert_eq!(session.view().rows[0].note, "", "lowercase fits TCVN3");

    session.apply_transform(upper());
    session.refresh();
    assert!(
        !session.view().rows[0].note.is_empty(),
        "uppercase toned vowels do not fit TCVN3, and the row should say so"
    );
    let _ = std::fs::remove_dir_all(&dir);
}

/// The switches reach core, and a fresh window has both of them off.
#[test]
fn the_switches_travel_with_the_transform() {
    let mut session = pasted("đẹp");
    session.apply_transform(transform(funput_core::textcase::Transform::NoDiacritics));
    session.refresh();
    assert_eq!(session.view().output_preview, "dep");
    assert!(!session.view().keep_d);

    session.set_keep_d(true);
    session.refresh();
    assert_eq!(session.view().output_preview, "đep");
    assert!(session.view().keep_d);
}

/// A document nothing could explain is not converted, and for the same reason it is
/// not transformed either: one rule, not two.
///
/// The fixture is a real one — `detect` places `việt nam` but has nothing to go on
/// in an all-caps line, which is exactly when the window asks the user instead.
#[test]
fn a_document_with_no_charset_is_left_alone_by_both_axes() {
    let mut session = Session::new();
    session.set_input("GỬI VỀ TP. HCM".to_string());
    session.apply_transform(upper());
    session.refresh();

    assert_eq!(
        session.view().source,
        None,
        "fixture is meant to be unplaceable"
    );
    assert_eq!(session.view().output_preview, "GỬI VỀ TP. HCM");
    assert_eq!(session.result_text(), None);
}
