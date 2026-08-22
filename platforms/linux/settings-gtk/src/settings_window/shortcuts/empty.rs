//! Empty-state for the shortcuts page.

use adw::prelude::*;
use adw::StatusPage;
use gtk::{Align, Button};

pub(super) fn page(on_add: impl Fn() + 'static) -> StatusPage {
    let page = StatusPage::builder()
        .icon_name("edit-find-replace-symbolic")
        .title("Chưa có gõ tắt")
        .description("Gõ chữ tắt rồi dấu cách để bung — ví dụ vn → việt nam.")
        .build();
    let add = Button::builder()
        .label("Thêm gõ tắt")
        .halign(Align::Center)
        .build();
    add.add_css_class("suggested-action");
    add.connect_clicked(move |_| on_add());
    page.set_child(Some(&add));
    page
}
