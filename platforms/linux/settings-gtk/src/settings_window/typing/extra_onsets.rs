//! "Phụ âm đầu mở rộng": UniKey's single switch, then a checkbox per letter so the
//! user keeps only the ones they type. Turning the switch on ticks all four;
//! unticking the last one turns it off.
//!
//! As on Windows (`platforms/windows/src/ui/settings_callbacks/extra_onsets.rs`),
//! every click saves the new letter set, then [`Inner::show`] sets every widget from
//! it. `show` records the set before touching a widget, so the notifies it causes
//! compare equal in [`change::next`] and save nothing — no re-entrancy flag, and no
//! reliance on the file write having succeeded.

mod change;
mod hints;
mod letter;

use std::cell::{Cell, RefCell};
use std::rc::{Rc, Weak};

use adw::prelude::*;
use adw::{ExpanderRow, PreferencesGroup};
use gtk::CheckButton;

use crate::settings::{ExtraOnsetLetters, Method, OnsetLetter, Settings};
use change::Click;
use hints::Hints;

pub(super) struct Section {
    group: PreferencesGroup,
    inner: Rc<Inner>,
}

/// What the handlers reach. Holds the group's rows but not the group itself, which
/// is what lets the group own the one strong reference — see [`wire`].
struct Inner {
    expander: ExpanderRow,
    checks: Vec<(OnsetLetter, CheckButton)>,
    hints: Hints,
    method: Cell<Method>,
    /// The set on screen, which is also the set last saved.
    letters: Cell<ExtraOnsetLetters>,
}

impl Section {
    pub(super) fn new(settings: &Settings) -> Self {
        let expander = ExpanderRow::builder()
            .title("Cho phép z, f, w, j đầu từ")
            .subtitle("Gõ teencode, từ mượn và tên riêng: zô, fải, wá, jờ.")
            .show_enable_switch(true)
            .build();
        let checks = OnsetLetter::ALL
            .into_iter()
            .map(|letter| {
                let (row, check) = letter::row(letter);
                expander.add_row(&row);
                (letter, check)
            })
            .collect();
        let hints = Hints::new();
        expander.add_row(hints.row());

        let group = PreferencesGroup::builder()
            .title("Phụ âm đầu mở rộng")
            .build();
        group.add(&expander);

        let inner = Rc::new(Inner {
            expander,
            checks,
            hints,
            method: Cell::new(settings.method),
            letters: Cell::new(settings.extra_onsets),
        });
        // Show first, wire second: the initial state is not a click.
        inner.show(settings.extra_onsets);
        wire(&inner, &group);
        Self { group, inner }
    }

    pub(super) fn group(&self) -> &PreferencesGroup {
        &self.group
    }

    /// For the method radios on this page: the `ww` note depends on the method.
    pub(super) fn method_listener(&self) -> impl Fn(Method) + Clone + 'static {
        let weak = Rc::downgrade(&self.inner);
        move |method| {
            if let Some(inner) = weak.upgrade() {
                inner.method.set(method);
                inner.hints.update(method, inner.letters.get());
            }
        }
    }
}

fn wire(inner: &Rc<Inner>, group: &PreferencesGroup) {
    let weak = Rc::downgrade(inner);
    inner.expander.connect_enable_expansion_notify(move |row| {
        click(&weak, Click::Switch(row.enables_expansion()));
    });
    for (letter, check) in &inner.checks {
        let (letter, weak) = (*letter, Rc::downgrade(inner));
        check.connect_active_notify(move |check| {
            click(&weak, Click::Letter(letter, check.is_active()));
        });
    }
    // Re-read on every visit: the method can change outside this page (onboarding).
    let weak = Rc::downgrade(inner);
    group.connect_map(move |_| {
        if let Some(inner) = weak.upgrade() {
            let settings = Settings::load();
            inner.method.set(settings.method);
            inner.show(settings.extra_onsets);
        }
    });
    // Every handler above holds a `Weak` — the convert window's rule
    // (`convert/open.rs`). The strong reference rides on the group, which `Inner`
    // does not own: the section lives exactly as long as its widgets, and a closed
    // window is not kept alive by a cycle through them.
    let keep = RefCell::new(Some(Rc::clone(inner)));
    group.connect_destroy(move |_| drop(keep.take()));
}

fn click(weak: &Weak<Inner>, click: Click) {
    let Some(inner) = weak.upgrade() else {
        return;
    };
    let Some(letters) = change::next(inner.letters.get(), click) else {
        return;
    };
    Settings::update(|settings| settings.extra_onsets = letters);
    inner.show(letters);
}

impl Inner {
    /// Set the switch, every checkbox and the notes from `letters`.
    fn show(&self, letters: ExtraOnsetLetters) {
        self.letters.set(letters);
        for (letter, check) in &self.checks {
            check.set_active(letters.contains(*letter));
        }
        self.expander.set_enable_expansion(!letters.is_empty());
        self.hints.update(self.method.get(), letters);
    }
}
