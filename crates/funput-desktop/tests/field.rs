//! The gõ tắt expansion field in Windows Settings composes at the caret, not at
//! the end of the text (issue #582).
//!
//! [`Field`] stands in for Slint's `TextInput`: it holds the text and the caret and
//! anchor byte offsets, reports them with every key the way `ComposingInput` does,
//! and applies the edit that comes back.

use funput_config::{ExtraOnsetLetters, Method, Settings};
use funput_desktop::{FieldComposer, FieldView};

struct Field {
    composer: FieldComposer,
    text: String,
    cursor: usize,
    anchor: usize,
}

impl Field {
    /// Focus a field holding `text`, caret at the end.
    fn with(text: &str) -> Self {
        Self::with_settings(text, &telex())
    }

    fn with_settings(text: &str, settings: &Settings) -> Self {
        let mut composer = FieldComposer::new();
        composer.reset(settings);
        Self {
            composer,
            text: text.to_owned(),
            cursor: text.len(),
            anchor: text.len(),
        }
    }

    /// What `ComposingInput` sends with a key. Borrows only the field's own state,
    /// so the composer can be borrowed mutably alongside it.
    fn view<'a>(text: &'a str, cursor: usize, anchor: usize) -> FieldView<'a> {
        FieldView {
            text,
            cursor,
            anchor,
        }
    }

    /// A click: the caret lands before `marker`'s first occurrence.
    fn click_before(&mut self, marker: &str) {
        let at = self.text.find(marker).expect("marker in the field");
        self.cursor = at;
        self.anchor = at;
    }

    fn click_at_end(&mut self) {
        self.cursor = self.text.len();
        self.anchor = self.text.len();
    }

    /// A drag over `marker`, from `anchor` to `cursor` ends as given.
    fn select(&mut self, marker: &str, backwards: bool) {
        let start = self.text.find(marker).expect("marker in the field");
        let end = start + marker.len();
        (self.anchor, self.cursor) = if backwards {
            (end, start)
        } else {
            (start, end)
        };
    }

    fn type_str(&mut self, keys: &str) {
        for key in keys.chars() {
            let edit = self
                .composer
                .key(Self::view(&self.text, self.cursor, self.anchor), key)
                .expect("text keys compose");
            self.text = edit.text;
            self.cursor = edit.cursor;
            self.anchor = edit.cursor;
        }
    }

    fn backspace(&mut self) {
        let edit = self
            .composer
            .backspace(Self::view(&self.text, self.cursor, self.anchor));
        self.text = edit.text;
        self.cursor = edit.cursor;
        self.anchor = edit.cursor;
    }

    /// The text with `|` at the caret.
    fn shown(&self) -> String {
        let mut shown = self.text.clone();
        shown.insert(self.cursor, '|');
        shown
    }
}

fn telex() -> Settings {
    Settings {
        method: Method::Telex,
        ..Settings::default()
    }
}

#[test]
fn typing_at_the_end_composes_as_before() {
    let mut field = Field::with("");
    field.type_str("cows tamas");
    assert_eq!(field.shown(), "cớ tấm|");
}

#[test]
fn typing_in_the_middle_lands_at_the_caret() {
    let mut field = Field::with("cơm Sài Gòn");
    field.click_before(" Sài");
    field.type_str(" tamas");
    assert_eq!(field.shown(), "cơm tấm| Sài Gòn");
}

#[test]
fn typing_at_the_start_lands_at_the_start() {
    let mut field = Field::with("tấm");
    field.click_before("tấm");
    field.type_str("cowm ");
    assert_eq!(field.shown(), "cơm |tấm");
}

/// A click after a word re-opens it, the way Backspace does in any app.
#[test]
fn a_tone_key_after_a_click_reaches_the_word_before_the_caret() {
    let mut field = Field::with("cơm tấm");
    field.click_before(" tấm");
    field.type_str("f");
    assert_eq!(field.shown(), "cờm| tấm");
}

#[test]
fn a_word_the_engine_will_not_take_stays_plain() {
    let mut field = Field::with("hello world");
    field.click_before(" world");
    field.type_str("s");
    assert_eq!(field.shown(), "hellos| world");
}

#[test]
fn typing_over_a_selection_replaces_it() {
    let mut field = Field::with("cơm tấm Sài Gòn");
    field.select("tấm", false);
    field.type_str("suwownf");
    assert_eq!(field.shown(), "cơm sườn| Sài Gòn");
}

#[test]
fn a_selection_made_right_to_left_is_replaced_the_same_way() {
    let mut field = Field::with("cơm tấm Sài Gòn");
    field.select("tấm", true);
    field.type_str("suwownf");
    assert_eq!(field.shown(), "cơm sườn| Sài Gòn");
}

#[test]
fn backspace_in_the_middle_deletes_before_the_caret() {
    let mut field = Field::with("cơm tấm");
    field.click_before(" tấm");
    field.backspace();
    assert_eq!(field.shown(), "cơ| tấm");
}

#[test]
fn backspace_deletes_a_selection_and_nothing_else() {
    let mut field = Field::with("cơm tấm Sài Gòn");
    field.select(" tấm", false);
    field.backspace();
    assert_eq!(field.shown(), "cơm| Sài Gòn");
}

/// Backspace over a space re-opens the word before it, like typing anywhere else.
#[test]
fn backspace_over_a_space_reopens_the_word() {
    let mut field = Field::with("");
    field.type_str("phur ");
    field.backspace();
    field.type_str("s");
    assert_eq!(field.shown(), "phú|");
}

/// The caret moves between two keys (a click): the second lands at the new spot.
#[test]
fn a_click_between_keys_moves_where_the_next_key_lands() {
    let mut field = Field::with("");
    field.type_str("cowm tamas");
    field.click_before(" tấm");
    field.type_str(" chay");
    assert_eq!(field.shown(), "cơm chay| tấm");
    field.click_at_end();
    field.type_str(" ddi");
    assert_eq!(field.shown(), "cơm chay tấm đi|");
}

/// Offsets from the toolkit can be stale or split a character; they are snapped
/// back onto the character's start instead of panicking.
#[test]
fn offsets_inside_a_character_or_past_the_end_are_snapped() {
    let mut field = Field::with("tấm");
    field.cursor = 2; // inside `ấ`, which is three bytes from byte 1
    field.anchor = 2;
    field.type_str("x");
    assert_eq!(field.text, "txấm");

    let mut field = Field::with("tấm");
    field.cursor = 99;
    field.anchor = 99;
    field.type_str(" ok");
    assert_eq!(field.shown(), "tấm ok|");
}

#[test]
fn a_modifier_key_leaves_the_field_alone() {
    let mut field = Field::with("cơm");
    field.select("cơm", false);
    let view = Field::view(&field.text, field.cursor, field.anchor);
    assert_eq!(field.composer.key(view, '\u{F701}'), None);
    assert_eq!(field.composer.key(view, '\u{1}'), None);
}

/// The gõ tắt field composes the way global typing does, extra onsets included.
#[test]
fn the_field_follows_the_chosen_onsets() {
    let typed = |letters: &str| {
        let settings = Settings {
            extra_onsets: ExtraOnsetLetters::from_id(letters),
            ..telex()
        };
        let mut field = Field::with_settings("", &settings);
        field.type_str("zoo jowf");
        field.text
    };
    assert_eq!(typed("z"), "zô jowf");
    assert_eq!(typed("zj"), "zô jờ");
}
