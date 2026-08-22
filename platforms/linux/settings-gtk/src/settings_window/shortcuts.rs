//! "Gõ tắt" page: manage text-expansion shortcuts (`vn` → `việt nam`, smart-cased to
//! `Vn` → `Việt Nam` / `VN` → `VIỆT NAM` at expansion time). Each shortcut
//! is an expander with two editable fields; edits persist by index and update the
//! header live, while add/delete rebuild the list.

use std::cell::{Cell, RefCell};
use std::rc::Rc;

use adw::prelude::*;
use adw::{EntryRow, ExpanderRow, PreferencesGroup, PreferencesPage};
use gtk::{Align, Button};

use crate::settings::{Settings, Shortcut};

mod empty;
mod row;

pub(super) fn page() -> gtk::Widget {
    let list_page = PreferencesPage::builder()
        .title("Gõ tắt")
        .icon_name("edit-find-replace-symbolic")
        .build();
    let group = PreferencesGroup::builder()
        .title("Gõ tắt")
        .description(
            "Gõ chữ tắt rồi dấu cách để bung — ví dụ vn → việt nam. Tự nhận diện hoa/thường: \
             Vn → Việt Nam, VN → VIỆT NAM.",
        )
        .build();
    list_page.add(&group);

    let pages = gtk::Stack::new();
    let rows: Rc<RefCell<Vec<gtk::Widget>>> = Rc::new(RefCell::new(Vec::new()));
    let focus_new = Rc::new(Cell::new(false));
    let rebuild: Rc<RefCell<Option<Rc<dyn Fn()>>>> = Rc::new(RefCell::new(None));

    let rebuild_impl: Rc<dyn Fn()> = {
        let group = group.clone();
        let pages = pages.clone();
        let rows = rows.clone();
        let focus_new = focus_new.clone();
        let rebuild = rebuild.clone();
        Rc::new(move || {
            for r in rows.borrow_mut().drain(..) {
                group.remove(&r);
            }
            let s = Settings::load();
            if s.shortcuts.is_empty() {
                pages.set_visible_child_name("empty");
                return;
            }
            pages.set_visible_child_name("list");
            let mut last: Option<(ExpanderRow, EntryRow)> = None;
            for (i, sc) in s.shortcuts.iter().enumerate() {
                let (expander, trigger) = row::build(i, sc, &rebuild);
                group.add(&expander);
                rows.borrow_mut().push(expander.clone().upcast());
                last = Some((expander, trigger));
            }
            if focus_new.replace(false) {
                if let Some((expander, trigger)) = last {
                    expander.set_expanded(true);
                    trigger.grab_focus();
                }
            }
        })
    };
    *rebuild.borrow_mut() = Some(rebuild_impl.clone());

    let add = {
        let rebuild = rebuild.clone();
        let focus_new = focus_new.clone();
        Rc::new(move || {
            Settings::update(|s| {
                s.shortcuts.push(Shortcut {
                    trigger: String::new(),
                    expansion: String::new(),
                });
            });
            focus_new.set(true);
            if let Some(f) = rebuild.borrow().as_ref() {
                f();
            }
        })
    };
    let add_header = add.clone();
    let add_btn = Button::builder()
        .icon_name("list-add-symbolic")
        .valign(Align::Center)
        .tooltip_text("Thêm gõ tắt")
        .build();
    add_btn.add_css_class("flat");
    add_btn.connect_clicked(move |_| add_header());
    group.set_header_suffix(Some(&add_btn));

    pages.add_named(&empty::page(move || add()), Some("empty"));
    pages.add_named(&list_page, Some("list"));
    rebuild_impl();
    pages.upcast()
}
