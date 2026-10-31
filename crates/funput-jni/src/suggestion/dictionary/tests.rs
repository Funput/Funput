//! What Kotlin decodes, tested without a JVM: the codes, the flattening, and the
//! app's round trip through the inbox.

use funput_suggestions::{
    DictionaryEntry, DictionaryFilter, DictionarySort, Edit, EditKind, EditOutcome, Origin,
    post_edits,
};

use super::codes;
use super::list::listing;
use crate::suggestion::registry;

#[test]
fn every_code_maps_and_an_unknown_one_is_refused() {
    let kinds = [
        EditKind::Add,
        EditKind::Forget,
        EditKind::Block,
        EditKind::Unblock,
    ];
    for (code, kind) in kinds.into_iter().enumerate() {
        assert_eq!(codes::edit_kind(code as i32), Some(kind));
    }
    assert_eq!(codes::edit_kind(4), None);
    assert_eq!(codes::filter(1), Some(DictionaryFilter::Learned));
    assert_eq!(codes::filter(-1), None);
    assert_eq!(codes::sort(2), Some(DictionarySort::Alphabetical));
    assert_eq!(codes::sort(3), None);
    assert_eq!(
        codes::outcome(EditOutcome::LimitReached),
        codes::LIMIT_REACHED
    );
}

#[test]
fn a_listing_flattens_into_parallel_arrays() {
    let entries = [
        DictionaryEntry {
            word: "được",
            uses: 7,
            last_used: 9,
            origin: Origin::Learned,
        },
        DictionaryEntry {
            word: "funput",
            uses: 2,
            last_used: 3,
            origin: Origin::Added,
        },
    ];
    let (words, details) = codes::flatten(&entries);
    assert_eq!(words, ["được", "funput"]);
    assert_eq!(
        details,
        [7, 9, codes::ORIGIN_LEARNED, 2, 3, codes::ORIGIN_ADDED]
    );
    assert_eq!(details.len(), words.len() * codes::DETAILS_PER_ENTRY);
}

#[test]
fn the_app_sees_its_posted_edits_and_the_keyboard_drains_them() {
    let directory = tempfile::tempdir().unwrap();
    let keyboard = registry::open(directory.path()).unwrap();
    post_edits(directory.path(), &[Edit::new(EditKind::Add, "funput")]).unwrap();

    let view = registry::open_snapshot(directory.path()).unwrap();
    let (words, _) = listing(view, 2, 0, "").unwrap();
    assert_eq!(words, ["funput"]);
    let preview = registry::with_mut(view, |engine| {
        engine.apply_edit(Edit::new(EditKind::Add, "hai từ"))
    });
    assert_eq!(preview, Some(EditOutcome::Invalid));
    assert_eq!(listing(view, 9, 0, ""), None);

    let drained = registry::with_mut(keyboard, |engine| engine.apply_pending_edits().unwrap());
    assert_eq!(drained, Some(1));
    let (words, _) = listing(keyboard, 0, 0, "fun").unwrap();
    assert_eq!(words, ["funput"]);
    registry::destroy(view);
    registry::destroy(keyboard);
}
