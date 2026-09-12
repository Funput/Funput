//! The five transform chips.
//!
//! Buttons, not toggles. A press **stacks** a transform onto the list, and pressing the
//! one already on top is a no-op in the crate because every transform is idempotent. A
//! toggle would promise that the second click takes it back off — that is `Hoàn tác`'s
//! job, and not necessarily for the same transform — and it would flip itself before the
//! refresh flipped it back, which is a re-entrancy hole bought for nothing.
//!
//! Plain buttons in a `FlowBox`, not libadwaita's `pill`. The five Vietnamese names come
//! to roughly 530px of text and `.pill` adds 64px of padding to each, which would put
//! the row past this window's 880px default and well past its 640px minimum — and GTK
//! raises a window's minimum to fit its children, so the capsule that looks right on
//! macOS and Windows would leave Chuyển mã unable to shrink. A `FlowBox` wraps the row
//! instead, and only when the user makes the window narrow. `adw::WrapBox` is the
//! better container for this and needs no uniform columns, but it arrived in libadwaita
//! 1.7 and this crate targets 1.5 — worth revisiting when the floor moves.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::Convert;
use crate::convert::ui::widget;
use funput_convert::casing;

pub(super) struct Chips {
    pub(super) root: gtk::Box,
    chips: Vec<Chip>,
}

struct Chip {
    button: gtk::Button,
    tick: gtk::Image,
}

impl Chips {
    pub(super) fn new() -> Self {
        let flow = gtk::FlowBox::builder()
            .selection_mode(gtk::SelectionMode::None)
            .min_children_per_line(1)
            .max_children_per_line(casing::ALL.len() as u32)
            .homogeneous(false)
            .row_spacing(6)
            .column_spacing(6)
            .accessible_role(gtk::AccessibleRole::Group)
            .build();
        let chips: Vec<Chip> = casing::ALL
            .iter()
            .map(|&transform| chip(casing::name(transform)))
            .collect();
        for chip in &chips {
            flow.append(&chip.button);
        }

        // Aligned to the top, not centred: when the row wraps, the label belongs beside
        // the first line of chips rather than floating between the two.
        let label = widget::caption("Kiểu chữ");
        label.set_valign(gtk::Align::Start);
        label.set_margin_top(8);

        let root = gtk::Box::builder().spacing(8).build();
        root.append(&label);
        root.append(&flow);
        Self { root, chips }
    }

    pub(super) fn wire(&self, convert: &Rc<Convert>) {
        for (index, chip) in self.chips.iter().enumerate() {
            widget::click(&chip.button, convert, move |convert| {
                convert.session.borrow_mut().apply_transform(index);
                convert.refresh();
            });
        }
    }

    pub(super) fn refresh(&self, lit: &[bool]) {
        for (chip, &on) in self.chips.iter().zip(lit) {
            chip.tick.set_visible(on);
            // Bound rather than written inline: the two arms are arrays of different
            // length, and `set_css_classes` wants one slice type.
            let classes: &[&str] = if on { &["suggested-action"] } else { &[] };
            chip.button.set_css_classes(classes);
            // The tick is the cue for someone who cannot read the accent; this is the
            // one a screen reader reads. Either way, colour alone says nothing.
            chip.button
                .update_state(&[gtk::accessible::State::Pressed(if on {
                    gtk::AccessibleTristate::True
                } else {
                    gtk::AccessibleTristate::False
                })]);
        }
    }
}

/// One chip: a tick that appears once the transform is applied, then its name.
///
/// `object-select-symbolic` because it ships inside GTK's own icon resource as well as
/// in the system Adwaita theme. `check-plain-symbolic` and `emblem-ok-symbolic` are not
/// in Adwaita on 24.04 and would render as a missing image.
fn chip(name: &str) -> Chip {
    let tick = gtk::Image::builder()
        .icon_name("object-select-symbolic")
        .pixel_size(12)
        .visible(false)
        .build();
    let content = gtk::Box::builder().spacing(6).build();
    content.append(&tick);
    content.append(&gtk::Label::new(Some(name)));

    // `Align::Start` because a `FlowBox` gives every cell in a wrapped row the width of
    // its widest child. Without this, `CHỮ HOA` would be a wide button at 640px and a
    // narrow one at 880px — a chip that changes size with the window is a chip that
    // looks like a different control.
    let button = gtk::Button::builder()
        .child(&content)
        .halign(gtk::Align::Start)
        .build();
    Chip { button, tick }
}
