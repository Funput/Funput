//! How the composing word reaches the client (preedit vs document repair).

use adw::prelude::*;
use adw::{PreferencesGroup, SwitchRow};

use crate::settings::Settings;

/// Both Linux shells perform an `Effect::Replace`. A client that cannot report
/// surrounding text keeps the preedit, so the switch is never a no-op.
pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder()
        .title("Cách chữ vào ứng dụng")
        .build();
    let row = SwitchRow::builder()
        .title("Gõ thẳng vào ứng dụng (thử nghiệm)")
        .subtitle(
            "Chữ đi thẳng vào tài liệu thay vì nằm ở dòng gạch chân, nên không mất khi \
             đổi cửa sổ giữa chừng. Ứng dụng nào không hỗ trợ thì tự quay về cách cũ.",
        )
        .active(settings.non_preedit)
        .build();
    row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.non_preedit = on);
    });
    group.add(&row);
    group
}
