//! Brand header for Tổng quan — same left-aligned strip as Windows / Giới thiệu.

use adw::prelude::*;
use gtk::{Align, Orientation};

pub(super) fn widget() -> gtk::Widget {
    let row = gtk::Box::new(Orientation::Horizontal, 16);
    row.set_halign(Align::Start);
    row.set_margin_bottom(4);

    let logo = gtk::Image::from_icon_name("funput");
    logo.set_pixel_size(64);
    logo.set_valign(Align::Center);

    let text = gtk::Box::new(Orientation::Vertical, 4);
    text.set_valign(Align::Center);
    let title = gtk::Label::new(Some("Funput"));
    title.add_css_class("title-1");
    title.set_halign(Align::Start);
    let subtitle = gtk::Label::new(Some("Gõ tiếng Việt ở mọi ứng dụng trên Linux."));
    subtitle.add_css_class("dim-label");
    subtitle.set_wrap(true);
    subtitle.set_halign(Align::Start);
    text.append(&title);
    text.append(&subtitle);

    row.append(&logo);
    row.append(&text);
    row.upcast()
}
