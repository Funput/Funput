use std::collections::HashMap;

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
        },
        buffer: buffer.into(),
        keys: keys.into(),
        cap_sentence_ended: false,
        cap_armed: false,
        shortcuts: HashMap::new(),
        vn_form: String::new(),
        restore_override: None,
    }
}

mod commit;
mod restore;
