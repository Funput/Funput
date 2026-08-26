//! The sidebar's pages. Adding one is a variant here plus a `ViewStack` child.

#[derive(Clone, Copy, PartialEq, Eq)]
pub(in crate::settings_window) enum Destination {
    Overview,
    Typing,
    Keyboard,
    Shortcuts,
    About,
}

impl Destination {
    pub(in crate::settings_window) const ALL: [Self; 5] = [
        Self::Overview,
        Self::Typing,
        Self::Keyboard,
        Self::Shortcuts,
        Self::About,
    ];

    pub(in crate::settings_window) const fn id(self) -> &'static str {
        match self {
            Self::Overview => "overview",
            Self::Typing => "typing",
            Self::Keyboard => "keyboard",
            Self::Shortcuts => "shortcuts",
            Self::About => "about",
        }
    }

    pub(in crate::settings_window) const fn title(self) -> &'static str {
        match self {
            Self::Overview => "Tổng quan",
            Self::Typing => "Cách gõ",
            Self::Keyboard => "Phím tắt",
            Self::Shortcuts => "Gõ tắt",
            Self::About => "Giới thiệu",
        }
    }

    pub(in crate::settings_window) const fn icon(self) -> &'static str {
        match self {
            Self::Overview => "preferences-system-symbolic",
            Self::Typing => "input-keyboard-symbolic",
            Self::Keyboard => "preferences-desktop-keyboard-shortcuts-symbolic",
            Self::Shortcuts => "edit-find-replace-symbolic",
            Self::About => "help-about-symbolic",
        }
    }

    pub(super) fn from_id(id: &str) -> Option<Self> {
        Self::ALL.into_iter().find(|dest| dest.id() == id)
    }
}
