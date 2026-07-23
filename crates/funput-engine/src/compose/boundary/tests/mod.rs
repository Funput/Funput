use std::collections::HashMap;

use funput_core::{InputMethod, ToneStyle};

use crate::model::Session;

fn session(method: InputMethod, buffer: &str, keys: &str) -> Session {
    Session {
        enabled: true,
        method,
        tone_style: ToneStyle::Traditional,
        buffer: buffer.into(),
        keys: keys.into(),
        smart_restore: true,
        eager_restore: true,
        spell_check: false,
        auto_capitalize: false,
        cap_sentence_ended: false,
        cap_armed: false,
        shortcuts: HashMap::new(),
        vn_form: String::new(),
        restore_override: None,
    }
}

mod commit;
mod restore;
