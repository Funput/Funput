//! "Giới thiệu" page — brand, version, and links (not a dialog).

use adw::prelude::*;
use adw::{ActionRow, Clamp, PreferencesGroup};
use gtk::gio;
use gtk::{Align, Justification, Orientation, PolicyType};

const WEBSITE: &str = "https://funput.app/";
const GITHUB: &str = "https://github.com/Funput/Funput";
const ISSUES: &str = "https://github.com/Funput/Funput/issues";

pub fn page() -> gtk::Widget {
    let column = gtk::Box::new(Orientation::Vertical, 24);
    column.set_margin_top(24);
    column.set_margin_bottom(24);
    column.set_margin_start(12);
    column.set_margin_end(12);
    column.append(&hero());
    column.append(&links());

    let clamp = Clamp::new();
    clamp.set_maximum_size(600);
    clamp.set_child(Some(&column));
    let scroll = gtk::ScrolledWindow::new();
    scroll.set_policy(PolicyType::Never, PolicyType::Automatic);
    scroll.set_child(Some(&clamp));
    scroll.upcast()
}

fn hero() -> gtk::Widget {
    let row = gtk::Box::new(Orientation::Horizontal, 16);
    row.set_halign(Align::Start);

    let logo = gtk::Image::from_icon_name("funput");
    logo.set_pixel_size(72);
    logo.set_valign(Align::Center);

    let text = gtk::Box::new(Orientation::Vertical, 4);
    text.set_valign(Align::Center);
    let title = gtk::Label::new(Some("Funput"));
    title.add_css_class("title-1");
    title.set_halign(Align::Start);
    let tagline = gtk::Label::new(Some("Bộ gõ tiếng Việt — miễn phí, mã nguồn mở."));
    tagline.add_css_class("dim-label");
    tagline.set_wrap(true);
    tagline.set_justify(Justification::Left);
    tagline.set_halign(Align::Start);
    let version = gtk::Label::new(Some(&format!("Phiên bản {}", env!("CARGO_PKG_VERSION"))));
    version.add_css_class("caption");
    version.add_css_class("dim-label");
    version.set_halign(Align::Start);
    text.append(&title);
    text.append(&tagline);
    text.append(&version);

    row.append(&logo);
    row.append(&text);
    row.upcast()
}

fn links() -> PreferencesGroup {
    let group = PreferencesGroup::builder().title("Liên kết").build();
    group.add(&link_row(
        "Trang web",
        "Trang chủ funput.app.",
        "web-browser-symbolic",
        WEBSITE,
    ));
    group.add(&link_row(
        "GitHub",
        "Mã nguồn của Funput.",
        "folder-remote-symbolic",
        GITHUB,
    ));
    group.add(&link_row(
        "Báo lỗi",
        "Gửi lỗi hoặc góp ý cho nhóm làm Funput.",
        "dialog-information-symbolic",
        ISSUES,
    ));
    group
}

fn link_row(title: &str, subtitle: &str, icon: &str, uri: &'static str) -> ActionRow {
    let row = ActionRow::builder()
        .title(title)
        .subtitle(subtitle)
        .activatable(true)
        .build();
    row.add_prefix(&gtk::Image::from_icon_name(icon));
    row.add_suffix(&gtk::Image::from_icon_name("adw-external-link-symbolic"));
    row.connect_activated(move |_| {
        let _ = gio::AppInfo::launch_default_for_uri(uri, None::<&gio::AppLaunchContext>);
    });
    row
}
