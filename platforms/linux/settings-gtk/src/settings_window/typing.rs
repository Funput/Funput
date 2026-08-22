//! "Cách gõ" page: how keys become Vietnamese.
//! Each group lives in `typing/` so a later option is one file plus one `page.add`.

mod method;
mod smart;

use adw::prelude::*;
use adw::PreferencesPage;

use crate::settings::Settings;

pub(super) fn page() -> PreferencesPage {
    let settings = Settings::load();
    let page = PreferencesPage::builder()
        .title("Cách gõ")
        .icon_name("input-keyboard-symbolic")
        .build();
    page.add(&method::group(&settings));
    page.add(&smart::group(&settings));
    page
}
