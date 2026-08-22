//! "Tổng quan" — brand, featured delivery, status jumps, VI/EN switch.

mod brand;
mod delivery;
mod enabled;
mod status;

use adw::prelude::*;
use adw::{Clamp, NavigationSplitView, ViewStack, WindowTitle};
use gtk::{Orientation, PolicyType};

use crate::settings::Settings;

pub(super) fn page(
    stack: &ViewStack,
    split: &NavigationSplitView,
    title: &WindowTitle,
) -> gtk::Widget {
    let settings = Settings::load();
    let column = gtk::Box::new(Orientation::Vertical, 24);
    column.set_margin_top(24);
    column.set_margin_bottom(24);
    column.set_margin_start(12);
    column.set_margin_end(12);
    column.append(&brand::widget());
    column.append(&delivery::group(&settings));
    column.append(&status::group(stack, split, title));
    column.append(&enabled::group(&settings));

    let clamp = Clamp::new();
    clamp.set_maximum_size(600);
    clamp.set_child(Some(&column));

    let scroll = gtk::ScrolledWindow::new();
    scroll.set_policy(PolicyType::Never, PolicyType::Automatic);
    scroll.set_child(Some(&clamp));
    scroll.upcast()
}
