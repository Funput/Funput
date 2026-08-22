//! "Tổng quan" — status rows that jump to other pages, plus the VI/EN switch.

mod enabled;
mod status;

use adw::prelude::*;
use adw::{NavigationSplitView, PreferencesPage, ViewStack, WindowTitle};

use crate::settings::Settings;

pub(super) fn page(
    stack: &ViewStack,
    split: &NavigationSplitView,
    title: &WindowTitle,
) -> PreferencesPage {
    let settings = Settings::load();
    let page = PreferencesPage::builder()
        .title("Tổng quan")
        .icon_name("preferences-system-symbolic")
        .build();
    page.add(&status::group(stack, split, title));
    page.add(&enabled::group(&settings));
    page
}
