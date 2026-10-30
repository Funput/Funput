//! The app's side of the store: it reads without writing, and posts edits the
//! keyboard applies.

use std::fs;
use std::path::Path;
use std::time::SystemTime;

use tempfile::tempdir;

use super::{blocked, listed};
use crate::persistence::MAX_INBOX_FILES;
use crate::tests::learned;
use crate::{Edit, EditKind, SuggestionConfig, SuggestionEngine, post_edits};

fn opened(directory: &Path) -> SuggestionEngine {
    SuggestionEngine::open(directory, SuggestionConfig::default()).unwrap()
}

fn inbox(directory: &Path) -> Vec<String> {
    let Ok(entries) = fs::read_dir(directory.join("edits")) else {
        return Vec::new();
    };
    let mut names: Vec<_> = entries
        .map(|entry| entry.unwrap().file_name().to_string_lossy().into_owned())
        .collect();
    names.sort();
    names
}

/// Every file under `directory` with its length and modification time.
fn listing(directory: &Path) -> Vec<(String, u64, SystemTime)> {
    let mut files = Vec::new();
    let mut pending = vec![directory.to_owned()];
    while let Some(path) = pending.pop() {
        for entry in fs::read_dir(path).unwrap() {
            let entry = entry.unwrap();
            let metadata = entry.metadata().unwrap();
            if metadata.is_dir() {
                pending.push(entry.path());
            }
            let name = entry.path().display().to_string();
            files.push((name, metadata.len(), metadata.modified().unwrap()));
        }
    }
    files.sort();
    files
}

#[test]
fn posted_edits_are_applied_on_open_and_then_removed() {
    let directory = tempdir().unwrap();
    {
        let mut engine = opened(directory.path());
        learned(&mut engine, "xin", 2);
        engine.flush().unwrap();
    }
    post_edits(directory.path(), &[Edit::new(EditKind::Add, "funput")]).unwrap();
    post_edits(directory.path(), &[Edit::new(EditKind::Block, "xin")]).unwrap();
    assert_eq!(inbox(directory.path()).len(), 2);

    let engine = opened(directory.path());
    assert_eq!(listed(&engine), ["funput"]);
    assert_eq!(blocked(&engine), ["xin"]);
    assert!(inbox(directory.path()).is_empty());
    drop(engine);
    // Durable in the journal now, not only in that engine's memory.
    assert_eq!(listed(&opened(directory.path())), ["funput"]);
}

#[test]
fn an_open_keyboard_drains_the_inbox_on_request() {
    let directory = tempdir().unwrap();
    let mut keyboard = opened(directory.path());
    post_edits(directory.path(), &[Edit::new(EditKind::Add, "funput")]).unwrap();
    assert_eq!(keyboard.apply_pending_edits().unwrap(), 1);
    assert_eq!(listed(&keyboard), ["funput"]);
    assert_eq!(keyboard.apply_pending_edits().unwrap(), 0);
}

#[test]
fn a_damaged_file_is_dropped_and_the_rest_still_apply() {
    let directory = tempdir().unwrap();
    post_edits(directory.path(), &[Edit::new(EditKind::Add, "funput")]).unwrap();
    let edits = directory.path().join("edits");
    let posted = edits.join(&inbox(directory.path())[0]);
    let mut torn = fs::read(&posted).unwrap();
    torn.pop();
    fs::write(edits.join("0-damaged.edit"), torn).unwrap();
    fs::write(edits.join("1-in-progress.tmp"), b"partial").unwrap();

    let engine = opened(directory.path());
    assert_eq!(listed(&engine), ["funput"]);
    assert_eq!(inbox(directory.path()), ["1-in-progress.tmp"]);
}

#[test]
fn the_app_sees_pending_edits_and_writes_nothing() {
    let directory = tempdir().unwrap();
    {
        let mut engine = opened(directory.path());
        learned(&mut engine, "xin", 2);
        learned(&mut engine, "chào", 2);
        engine.flush().unwrap();
    }
    post_edits(directory.path(), &[Edit::new(EditKind::Forget, "xin")]).unwrap();
    fs::write(directory.path().join("edits").join("0-damaged.edit"), b"x").unwrap();
    let before = listing(directory.path());

    let mut view =
        SuggestionEngine::open_snapshot(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(listed(&view), ["chào"]);
    view.flush().unwrap();
    view.compact().unwrap();
    assert_eq!(view.apply_pending_edits().unwrap(), 0);
    assert_eq!(listing(directory.path()), before);
}

#[test]
fn the_app_can_read_a_store_that_does_not_exist_yet() {
    let directory = tempdir().unwrap();
    let missing = directory.path().join("never-opened");
    let view = SuggestionEngine::open_snapshot(&missing, SuggestionConfig::default()).unwrap();
    assert!(listed(&view).is_empty());
    assert!(!missing.exists());
}

#[test]
fn a_file_from_a_newer_build_is_left_for_that_build() {
    let directory = tempdir().unwrap();
    post_edits(directory.path(), &[Edit::new(EditKind::Add, "funput")]).unwrap();
    let edits = directory.path().join("edits");
    let posted = edits.join(&inbox(directory.path())[0]);
    let mut newer = fs::read(&posted).unwrap();
    newer[4..6].copy_from_slice(&u16::MAX.to_le_bytes());
    fs::write(edits.join("0-newer.edit"), newer).unwrap();

    let engine = opened(directory.path());
    assert_eq!(listed(&engine), ["funput"]);
    assert_eq!(inbox(directory.path()), ["0-newer.edit"]);
}

#[test]
fn a_drain_takes_the_oldest_files_first_and_leaves_the_rest() {
    let directory = tempdir().unwrap();
    let words: Vec<String> = (0..=MAX_INBOX_FILES).map(|n| format!("w{n:03}")).collect();
    for word in &words {
        post_edits(directory.path(), &[Edit::new(EditKind::Add, word)]).unwrap();
    }
    let mut keyboard = opened(directory.path());
    assert_eq!(listed(&keyboard).len(), MAX_INBOX_FILES);
    assert!(!listed(&keyboard).contains(&words[MAX_INBOX_FILES]));
    assert_eq!(keyboard.apply_pending_edits().unwrap(), 1);
    assert!(listed(&keyboard).contains(&words[MAX_INBOX_FILES]));
}

#[test]
fn a_word_no_store_can_hold_is_refused_before_anything_is_written() {
    let directory = tempdir().unwrap();
    let long = "a".repeat(33);
    let edits = [
        Edit::new(EditKind::Add, "funput"),
        Edit::new(EditKind::Add, &long),
    ];
    let error = post_edits(directory.path(), &edits).unwrap_err();
    assert_eq!(error.kind(), std::io::ErrorKind::InvalidInput);
    assert!(inbox(directory.path()).is_empty());
}

#[test]
fn a_large_post_is_split_into_files_that_all_apply_in_order() {
    let directory = tempdir().unwrap();
    let words: Vec<String> = (0..600).map(|n| format!("w{n:03}")).collect();
    let mut edits: Vec<_> = words
        .iter()
        .map(|word| Edit::new(EditKind::Block, word))
        .collect();
    edits.push(Edit::new(EditKind::Unblock, "w000"));
    post_edits(directory.path(), &edits).unwrap();
    assert_eq!(inbox(directory.path()).len(), 3);

    let engine = SuggestionEngine::open(
        directory.path(),
        SuggestionConfig {
            max_blocked_words: 1_000,
            ..SuggestionConfig::default()
        },
    )
    .unwrap();
    assert_eq!(blocked(&engine).len(), 599);
    assert!(!blocked(&engine).contains(&"w000"));
}
