//! Exclusive input-method check group (muda has no RadioMenuItem).

use funput_core::InputMethod;
use tray_icon::menu::{CheckMenuItem, Menu};

const TELEX_ID: &str = "telex";
const ADVANCED_ID: &str = "telex_advanced";
const VNI_ID: &str = "vni";

pub(super) struct MethodMenu {
    telex: CheckMenuItem,
    advanced: CheckMenuItem,
    vni: CheckMenuItem,
}

impl MethodMenu {
    pub(super) fn new(method: InputMethod) -> Self {
        let menu = Self {
            telex: item(TELEX_ID, "Telex"),
            advanced: item(ADVANCED_ID, "Telex nâng cao"),
            vni: item(VNI_ID, "VNI"),
        };
        menu.sync(method);
        menu
    }

    pub(super) fn append_to(&self, menu: &Menu) {
        menu.append_items(&[&self.telex, &self.advanced, &self.vni])
            .expect("append input methods");
    }

    pub(super) fn sync(&self, method: InputMethod) {
        let (telex, advanced, vni) = match method {
            InputMethod::Telex => (true, false, false),
            InputMethod::TelexAdvanced => (false, true, false),
            InputMethod::Vni => (false, false, true),
            _ => (true, false, false),
        };
        self.telex.set_checked(telex);
        self.advanced.set_checked(advanced);
        self.vni.set_checked(vni);
    }
}

pub(super) fn method_for_id(id: &str) -> Option<InputMethod> {
    match id {
        TELEX_ID => Some(InputMethod::Telex),
        ADVANCED_ID => Some(InputMethod::TelexAdvanced),
        VNI_ID => Some(InputMethod::Vni),
        _ => None,
    }
}

fn item(id: &str, label: &str) -> CheckMenuItem {
    CheckMenuItem::with_id(id, label, true, false, None)
}
