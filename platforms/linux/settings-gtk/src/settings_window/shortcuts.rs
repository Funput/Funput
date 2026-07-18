//! "Gõ tắt" page: manage text-expansion shortcuts (`vn` → `Việt Nam`). Each shortcut
//! is an expander with two editable fields; edits persist by index and update the
//! header live, while add/delete rebuild the list.

use std::cell::{Cell, RefCell};
use std::rc::Rc;

use adw::prelude::*;
use adw::{ActionRow, EntryRow, ExpanderRow, PreferencesGroup, PreferencesPage};
use gtk::{Align, Button};

use crate::settings::{Settings, Shortcut};

mod row;

pub(super) fn page() -> PreferencesPage {
    let page = PreferencesPage::builder()
        .title("Gõ tắt")
        .icon_name("edit-find-replace-symbolic")
        .build();

    let group = PreferencesGroup::builder()
        .title("Gõ tắt")
        .description("Gõ chữ tắt rồi dấu cách để bung — ví dụ vn → Việt Nam. Phân biệt hoa/thường.")
        .build();
    page.add(&group);

    // Rows we added, removed before each rebuild (AdwPreferencesGroup has no clear).
    let rows: Rc<RefCell<Vec<gtk::Widget>>> = Rc::new(RefCell::new(Vec::new()));
    // Set when the user just added a row, so the rebuild expands + focuses it.
    let focus_new = Rc::new(Cell::new(false));
    // Wrapped so the per-row delete buttons created inside the rebuild can re-run it.
    let rebuild: Rc<RefCell<Option<Rc<dyn Fn()>>>> = Rc::new(RefCell::new(None));

    let rebuild_impl: Rc<dyn Fn()> = {
        let group = group.clone();
        let rows = rows.clone();
        let focus_new = focus_new.clone();
        let rebuild = rebuild.clone();
        Rc::new(move || {
            for r in rows.borrow_mut().drain(..) {
                group.remove(&r);
            }

            let s = Settings::load();
            if s.shortcuts.is_empty() {
                let row = ActionRow::builder()
                    .title("Chưa có gõ tắt nào")
                    .subtitle("Bấm Thêm để tạo — ví dụ vn → Việt Nam, kg → không.")
                    .build();
                group.add(&row);
                rows.borrow_mut().push(row.upcast());
                return;
            }

            let mut last: Option<(ExpanderRow, EntryRow)> = None;
            for (i, sc) in s.shortcuts.iter().enumerate() {
                let (expander, trigger) = row::build(i, sc, &rebuild);
                group.add(&expander);
                rows.borrow_mut().push(expander.clone().upcast());
                last = Some((expander, trigger));
            }

            // A freshly added row starts empty — open it and focus the trigger field.
            if focus_new.replace(false) {
                if let Some((expander, trigger)) = last {
                    expander.set_expanded(true);
                    trigger.grab_focus();
                }
            }
        })
    };
    *rebuild.borrow_mut() = Some(rebuild_impl.clone());

    // "Thêm" sits in the group header — append an empty shortcut, then rebuild.
    let add_btn = Button::builder()
        .icon_name("list-add-symbolic")
        .valign(Align::Center)
        .tooltip_text("Thêm gõ tắt")
        .build();
    add_btn.add_css_class("flat");
    {
        let rebuild = rebuild.clone();
        let focus_new = focus_new.clone();
        add_btn.connect_clicked(move |_| {
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
        });
    }
    group.set_header_suffix(Some(&add_btn));

    rebuild_impl();
    page
}
