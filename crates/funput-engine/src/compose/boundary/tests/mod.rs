use std::collections::HashMap;

use funput_core::sentence::{Rules, Scanner};
use funput_core::{InputMethod, ToneStyle};

use crate::model::{EngineConfig, Session};

fn session(method: InputMethod, buffer: &str, keys: &str) -> Session {
    Session {
        enabled: true,
        config: EngineConfig {
            method,
            tone_style: ToneStyle::Traditional,
            smart_restore: true,
            eager_restore: true,
            spell_check: false,
            auto_capitalize: false,
            shortcuts_enabled: true,
            shortcut_smart_case: true,
            shortcuts_in_english: true,
        },
        buffer: buffer.into(),
        keys: keys.into(),
        scanner: Scanner::mid_text(Rules::TYPING),
        shortcuts: HashMap::new(),
        vn_form: String::new(),
        restore_override: None,
    }
}

mod commit;
mod restore;
