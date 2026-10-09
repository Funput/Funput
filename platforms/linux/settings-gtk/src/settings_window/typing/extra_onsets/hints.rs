//! The two notes under the letters: the trade every IME's switch makes, and — only
//! with Telex nâng cao and `w` admitted — how to reach the consonant `w` there.

use adw::prelude::*;
use gtk::{Label, ListBoxRow, Orientation};

use crate::settings::{ExtraOnsetLetters, Method, OnsetLetter};

// The flip key is off by default on Linux, so say where it lives rather than
// assume it works.
const TRADE_OFF: &str = "Từ tiếng Anh có vần tiếng Việt cũng được bỏ dấu (fast → fát). Gõ đúp \
                         phím dấu để giữ tiếng Anh (fasst → fast), hoặc bật \"Phím lật từ vừa \
                         gõ\" ở trang Phím tắt.";
const DOUBLE_W: &str = "Telex nâng cao: w vẫn là ư — gõ ww để có phụ âm w (wwas → wá).";

pub(super) struct Hints {
    row: ListBoxRow,
    double_w: Label,
}

impl Hints {
    pub(super) fn new() -> Self {
        let column = gtk::Box::builder()
            .orientation(Orientation::Vertical)
            .spacing(6)
            .margin_top(10)
            .margin_bottom(10)
            .margin_start(12)
            .margin_end(12)
            .build();
        let double_w = note(DOUBLE_W);
        column.append(&note(TRADE_OFF));
        column.append(&double_w);
        // A plain row: text to read, not something to click or focus.
        let row = ListBoxRow::builder()
            .child(&column)
            .activatable(false)
            .selectable(false)
            .focusable(false)
            .build();
        Self { row, double_w }
    }

    pub(super) fn row(&self) -> &ListBoxRow {
        &self.row
    }

    /// Show the `ww` note only when it is true for what the user has chosen.
    pub(super) fn update(&self, method: Method, letters: ExtraOnsetLetters) {
        self.double_w
            .set_visible(method == Method::TelexAdvanced && letters.contains(OnsetLetter::W));
    }
}

fn note(text: &str) -> Label {
    Label::builder()
        .label(text)
        .wrap(true)
        .xalign(0.0)
        .css_classes(["dim-label", "caption"])
        .build()
}
