//! About dialog. The sidebar calls [`present`]; a later chrome change can too.

use adw::prelude::*;
use adw::AboutDialog;
use gtk::glib::object::IsA;

pub(super) fn present(parent: &impl IsA<gtk::Widget>) {
    let about = AboutDialog::builder()
        .application_name("Funput")
        .application_icon("funput")
        .version(env!("CARGO_PKG_VERSION"))
        .comments("Bộ gõ tiếng Việt — miễn phí, mã nguồn mở.")
        .developer_name("Funput")
        .website("https://funput.app/")
        .issue_url("https://github.com/Funput/Funput/issues")
        .license_type(gtk::License::MitX11)
        .build();
    about.add_link("GitHub", "https://github.com/Funput/Funput");
    about.present(Some(parent));
}
