//! One letter of "Phụ âm đầu mở rộng": a checkbox, the letter on a keycap (it is
//! the key the user types), and what it lets them write — the Windows row
//! (`platforms/windows/ui/pages/typing/extra_onsets/checkbox.slint`) in Adwaita.

use adw::ActionRow;
use adw::prelude::*;
use gtk::{Align, CheckButton, Label, accessible};

use crate::settings::OnsetLetter;

/// The row for `letter` and its checkbox. The caller owns what the box means: this
/// only builds it, so the row has no rule of its own about the letter set.
pub(super) fn row(letter: OnsetLetter) -> (ActionRow, CheckButton) {
    let symbol = letter.symbol().to_string();
    let examples = letter.examples();

    let check = CheckButton::builder().valign(Align::Center).build();
    // The row's title is the examples, so name the letter for a screen reader too.
    check.update_property(&[accessible::Property::Label(&format!(
        "Phụ âm đầu {symbol}, ví dụ {examples}"
    ))]);
    let keycap = Label::builder()
        .label(&symbol)
        .valign(Align::Center)
        .css_classes(["keycap"])
        .build();

    let row = ActionRow::builder()
        .title(examples)
        .activatable(true)
        .build();
    // `add_prefix` prepends: the checkbox goes in last so it sits leftmost, then
    // the keycap — the Windows order.
    row.add_prefix(&keycap);
    row.add_prefix(&check);
    row.set_activatable_widget(Some(&check));
    (row, check)
}
