//! Brand header for Tổng quan — app icon from the packaged Funput theme.

use adw::prelude::*;
use gtk::{Align, Justification, Orientation};

pub(super) fn widget() -> gtk::Widget {
    let column = gtk::Box::new(Orientation::Vertical, 8);
    column.set_halign(Align::Center);
    column.set_margin_bottom(8);

    let logo = gtk::Image::from_icon_name("funput");
    logo.set_pixel_size(72);

    let title = gtk::Label::new(Some("Funput"));
    title.add_css_class("title-1");

    let subtitle = gtk::Label::new(Some("Gõ tiếng Việt ở mọi ứng dụng trên Linux."));
    subtitle.add_css_class("dim-label");
    subtitle.set_wrap(true);
    subtitle.set_justify(Justification::Center);

    column.append(&logo);
    column.append(&title);
    column.append(&subtitle);
    column.upcast()
}
