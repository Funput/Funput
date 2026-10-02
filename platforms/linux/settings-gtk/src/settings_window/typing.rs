//! "Cách gõ" page: how keys become Vietnamese.
//! Each group lives in `typing/` so a later option is one file plus one `page.add`.

mod extra_onsets;
mod method;
mod smart;

use adw::PreferencesPage;
use adw::prelude::*;

use crate::settings::Settings;

pub(super) fn page() -> PreferencesPage {
    let settings = Settings::load();
    let page = PreferencesPage::builder()
        .title("Cách gõ")
        .icon_name("input-keyboard-symbolic")
        .build();
    // Built first so the method radios can tell it which method is chosen: its
    // Telex nâng cao note depends on that. Placed last, as on Windows.
    let onsets = extra_onsets::Section::new(&settings);
    page.add(&method::group(&settings, onsets.method_listener()));
    page.add(&smart::group(&settings));
    page.add(onsets.group());
    page
}
