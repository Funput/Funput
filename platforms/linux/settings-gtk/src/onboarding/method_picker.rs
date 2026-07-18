use adw::prelude::*;
use gtk::{Align, Orientation};

use crate::settings::{Method, Settings};

pub(super) fn step() -> gtk::Box {
    let container = super::step(
        "⌨️",
        "Chọn kiểu gõ",
        "Có thể đổi bất cứ lúc nào trong Cài đặt.",
    );
    let selected = Settings::load().method;
    let picker = gtk::Box::new(Orientation::Horizontal, 0);
    picker.add_css_class("linked");
    picker.set_halign(Align::Center);

    let mut first: Option<gtk::ToggleButton> = None;
    for method in Method::ALL {
        let button = gtk::ToggleButton::with_label(method.label());
        if let Some(group) = &first {
            button.set_group(Some(group));
        } else {
            first = Some(button.clone());
        }
        button.set_active(method == selected);
        button.connect_toggled(move |button| {
            if button.is_active() {
                Settings::update(|settings| settings.method = method);
            }
        });
        picker.append(&button);
    }

    container.append(&picker);
    container
}
