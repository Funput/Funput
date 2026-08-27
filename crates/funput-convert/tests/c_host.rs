//! Driving a session the way a C host would, before there is a C host.
//!
//! macOS is Swift and cannot link this crate; its window will reach it through
//! `funput-ffi`, whose charset door already ships the text half. Before writing that
//! door it is worth proving the Rust side can actually cross it, because an ABI is
//! append-only — a shape settled wrongly is settled forever.
//!
//! So this file exports **nothing**. It reimplements the door's conventions over the
//! real API and drives them:
//!
//! - no `Charset`, no `PathBuf`, no `Option` and no borrow crosses; a charset is an
//!   index into `charset::ALL`, "not settled" is `-1`, and text goes out UTF-32;
//! - text writes are **all-or-nothing**: the length comes back whether or not it
//!   fit, and nothing is written unless all of it fits (`funput-ffi`'s
//!   `charset::write_text`, quoted in shape below);
//! - every collection is reachable by index with a `_count` beside it;
//! - a session that has been given nothing answers every question without panicking.
//!
//! What this cannot check is the `unsafe` marshalling itself. What it does check is
//! that the values exist in a shape that marshalling can reach — which is the half
//! that would be expensive to discover later.

use funput_convert::{Job, Mode, Scan, Session};

/// `funput-ffi`'s sizing rule, over a `&str` instead of a raw pointer.
fn write_text(text: &str, out: &mut [u32]) -> usize {
    let len = text.chars().count();
    if out.len() < len {
        return len;
    }
    for (slot, ch) in out.iter_mut().zip(text.chars()) {
        *slot = ch as u32;
    }
    len
}

/// Ask for the length, allocate, ask again — the two-call dance a host does.
fn read_text(text: &str) -> String {
    let needed = write_text(text, &mut []);
    let mut buffer = vec![0u32; needed];
    let written = write_text(text, &mut buffer);
    assert_eq!(written, needed, "the sizing call must predict the real one");
    buffer.iter().filter_map(|&c| char::from_u32(c)).collect()
}

/// `Option<usize>` has no C spelling. `-1` is "nothing settled yet", and it is an
/// answer rather than an error — the same thing `FUNPUT_CHARSET_UNKNOWN` means.
const NOT_SETTLED: i32 = -1;

fn position(value: Option<usize>) -> i32 {
    value
        .and_then(|i| i32::try_from(i).ok())
        .unwrap_or(NOT_SETTLED)
}

fn mode_code(mode: Mode) -> u32 {
    match mode {
        Mode::Empty => 0,
        Mode::Text => 1,
        Mode::Files => 2,
    }
}

fn scratch(name: &str, files: &[(&str, &[u8])]) -> Vec<std::path::PathBuf> {
    let dir = std::env::temp_dir().join(format!("funput-c-host-{name}"));
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).expect("scratch dir");
    files
        .iter()
        .map(|(name, bytes)| {
            let path = dir.join(name);
            std::fs::write(&path, bytes).expect("seed file");
            path
        })
        .collect()
}

/// The whole window, driven through nothing but integers and strings.
#[test]
fn a_host_can_run_the_window_without_a_single_rust_type() {
    let mut session = Session::new();

    // Build the menus. A host that spelled its own list would miss a charset added
    // later, so it asks — count, then name by index.
    let names = funput_convert::charset_names();
    assert!(names.len() >= 4);
    let named: Vec<String> = (0..names.len()).map(|i| read_text(names[i])).collect();
    assert!(named.iter().all(|n| !n.is_empty()));

    // Paste a paragraph: text in as UTF-32, a target as an index.
    let pasted: Vec<u32> = "việt nam".chars().map(|c| c as u32).collect();
    let text: String = pasted.iter().filter_map(|&c| char::from_u32(c)).collect();
    session.set_input(text);
    session.set_target(0);
    session.refresh();

    let view = session.view();
    assert_eq!(mode_code(view.mode), 1);
    assert_ne!(
        position(view.source),
        NOT_SETTLED,
        "ASCII-free Vietnamese reads"
    );
    assert!(!read_text(&view.output_preview).is_empty());
    // The left pane is the host's own buffer here, and the door must say so rather
    // than hand back a string the host would write over its user's caret.
    assert!(view.input_preview.is_none());

    // Copy: the whole document, not the capped preview.
    let copied = session.result_text().expect("something to copy");
    assert_eq!(read_text(&copied), copied);
}

/// Rows are reachable by index, with a count beside them, and the window's offset is
/// reported so a host can ask for the next slice.
#[test]
fn a_host_can_walk_the_batch_by_index() {
    let paths = scratch(
        "batch",
        &[
            ("a.txt", b"vi\xD6t nam"),
            ("b.txt", "việt nam".as_bytes()),
            ("c.txt", b"the quick \xFF brown fox jumps over"),
        ],
    );
    let mut session = Session::new();
    session.adopt(funput_convert::scan(&paths));
    session.set_row_window(0, 2);
    session.refresh();

    let view = session.view();
    assert_eq!(mode_code(view.mode), 2);
    assert_eq!(view.rows_total, 3, "the count is the whole batch");
    assert_eq!(view.rows.len(), 2, "the rows are the window");
    assert_eq!(view.rows_first, 0);
    for row in &view.rows {
        assert!(!read_text(&row.name).is_empty());
        let _: i32 = position(row.charset);
        let _: String = read_text(&row.note);
    }

    // The next slice, asked for the same way.
    session.set_row_window(2, 2);
    session.refresh();
    assert_eq!(session.view().rows_first, 2);
    assert_eq!(session.view().rows.len(), 1);
    assert_eq!(session.view().rows_total, 3);
}

/// A file nothing could read is reachable the same way, because a count cannot say
/// *which* one.
#[test]
fn a_host_can_name_what_could_not_be_read() {
    let paths = scratch("unreadable", &[("ok.txt", "việt".as_bytes())]);
    let mut session = Session::new();
    session.adopt(funput_convert::scan(&paths));
    session.refresh();

    let view = session.view();
    for file in &view.unreadable {
        assert!(!read_text(&file.name).is_empty());
        assert!(!read_text(&file.reason).is_empty());
    }
    let _: String = read_text(&funput_convert::unreadable_line(&view.unreadable));
}

/// A charset index a host stored from an older build, or invented, lands on a wrong
/// entry rather than taking the process down. Every door in `funput-ffi` answers a
/// bad index with a default; nothing here may panic first.
#[test]
fn an_index_a_host_made_up_is_answered_rather_than_fatal() {
    let mut session = Session::new();
    session.set_input("việt".to_string());
    session.set_target(usize::MAX);
    session.pick_source(Some(usize::MAX));
    session.pick_row_source(usize::MAX, Some(usize::MAX));
    session.set_row_window(usize::MAX, 0);
    session.refresh();

    // Every position the view reports has to be one a host can feed back into its
    // own menu, so the clamping happens where the index arrives.
    let view = session.view();
    assert!(view.target < funput_convert::charset_names().len());
    assert!(
        view.source
            .is_none_or(|i| i < funput_convert::charset_names().len())
    );
    assert!(view.rows.is_empty());
}

/// A handle that has been given nothing still answers everything — the shape of
/// `funput-ffi`'s `null_and_empty_inputs_are_safe`, one layer up.
#[test]
fn a_session_given_nothing_answers_every_question() {
    let mut session = Session::new();
    session.refresh();

    let view = session.view();
    assert_eq!(mode_code(view.mode), 0);
    assert_eq!(position(view.source), NOT_SETTLED);
    assert_eq!(view.rows_total, 0);
    assert_eq!(view.ready, 0);
    assert_eq!(read_text(&view.output_preview), "");
    assert_eq!(read_text(&view.warning), "");
    assert!(session.result_text().is_none());
    assert!(session.save_bytes().is_none());
    assert_eq!(session.batch_job().run().written, 0);
}

/// The two values a shell moves off its thread have to be able to go. Both shells do
/// it today and a C host will too — a future `Send` regression would be caught here
/// rather than in a shell that cannot be built on this machine.
#[test]
fn the_work_a_shell_hands_off_can_leave_the_thread() {
    fn assert_send<T: Send>() {}
    assert_send::<Scan>();
    assert_send::<Job>();
}
