//! "Gõ tắt" page: the two switches governing expansion, then the shortcuts
//! themselves (`vn` → `việt nam`, smart-cased to `Vn` → `Việt Nam` / `VN` →
//! `VIỆT NAM` at expansion time). Each shortcut is an expander with two editable
//! fields; edits persist by index and update the header live, while add/delete
//! rebuild the list.
//!
//! One page rather than a stack of empty/list states: the switches in `options` are
//! shown either way, which a swapped-out empty page could not do.

use std::cell::{Cell, RefCell};
use std::rc::Rc;

use adw::prelude::*;
use adw::{EntryRow, ExpanderRow, PreferencesGroup, PreferencesPage};
use gtk::{Align, Button};

use crate::settings::{Settings, Shortcut};

mod empty;
mod options;
mod row;

/// The "throw the list away and build it again" closure, held so the rows it builds
/// can call it.
///
/// `AdwPreferencesGroup` has no "remove every row", so a list that changes is rebuilt
/// by hand — and each row needs to reach the very closure that made it. That is a
/// cycle by nature, so it goes through a cell filled *after* the closure exists.
pub(super) type Rebuild = Rc<RefCell<Option<Rc<dyn Fn()>>>>;

pub(super) fn page() -> gtk::Widget {
    let settings = Settings::load();
    let page = PreferencesPage::builder()
        .title("Gõ tắt")
        .icon_name("edit-find-replace-symbolic")
        .build();
    let group = PreferencesGroup::builder()
        .title("Danh sách gõ tắt")
        .build();
    // The empty state is a group of its own rather than a separate page, so the
    // switches above it stay on screen while the table has no rows.
    let blank = PreferencesGroup::new();

    page.add(&options::group(&settings));
    page.add(&group);
    page.add(&blank);

    let rows: Rc<RefCell<Vec<gtk::Widget>>> = Rc::new(RefCell::new(Vec::new()));
    let focus_new = Rc::new(Cell::new(false));
    let rebuild: Rebuild = Rc::new(RefCell::new(None));

    let rebuild_impl: Rc<dyn Fn()> = {
        let group = group.clone();
        let blank = blank.clone();
        let rows = rows.clone();
        let focus_new = focus_new.clone();
        let rebuild = rebuild.clone();
        Rc::new(move || {
            for r in rows.borrow_mut().drain(..) {
                group.remove(&r);
            }
            let s = Settings::load();
            group.set_visible(!s.shortcuts.is_empty());
            blank.set_visible(s.shortcuts.is_empty());
            let mut last: Option<(ExpanderRow, EntryRow)> = None;
            for (i, sc) in s.shortcuts.iter().enumerate() {
                let (expander, trigger) = row::build(i, sc, &rebuild);
                group.add(&expander);
                rows.borrow_mut().push(expander.clone().upcast());
                last = Some((expander, trigger));
            }
            if focus_new.replace(false)
                && let Some((expander, trigger)) = last
            {
                expander.set_expanded(true);
                trigger.grab_focus();
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

    blank.add(&empty::page(move || add()));
    rebuild_impl();
    page.upcast()
}
