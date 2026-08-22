//! First onboarding page — brand icon instead of an emoji.

use adw::prelude::*;
use gtk::{Align, Justification, Orientation};

pub(super) fn step() -> gtk::Box {
    let column = gtk::Box::new(Orientation::Vertical, 12);
    column.set_valign(Align::Center);
    column.set_halign(Align::Center);
    column.set_margin_start(24);
    column.set_margin_end(24);

    let logo = gtk::Image::from_icon_name("funput");
    logo.set_pixel_size(72);

    let title = gtk::Label::new(Some("Chào mừng đến Funput"));
    title.add_css_class("title-2");

    let body = gtk::Label::new(Some(
        "Gõ tiếng Việt ở mọi nơi trên Linux — miễn phí, mã nguồn mở.",
    ));
    body.add_css_class("dim-label");
    body.set_wrap(true);
    body.set_justify(Justification::Center);

    column.append(&logo);
    column.append(&title);
    column.append(&body);
    column
}
