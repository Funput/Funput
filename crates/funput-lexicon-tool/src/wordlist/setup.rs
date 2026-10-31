//! The configurations a word is typed under when deciding whether to list it.

use funput_core::{ExtraOnsets, InputMethod, SyllableRules};
use funput_engine::{Engine, EngineConfig};

/// One configuration a word is typed under.
#[derive(Debug, Clone, Copy)]
pub(crate) struct Setup {
    pub(crate) method: InputMethod,
    pub(crate) rules: SyllableRules,
    /// Eager restore on: a word that dead-ends is shown as its raw keys before the
    /// next key, so a doubled key lands on raw letters and reverts nothing (`class`
    /// keeps both `s`). Off, the same key reverts the tone (`clá` + `s` → `clas`).
    pub(crate) eager: bool,
}

impl Setup {
    const fn new(method: InputMethod, rules: SyllableRules, eager: bool) -> Self {
        Self {
            method,
            rules,
            eager,
        }
    }

    /// A fresh engine in this setup, with `adjust` applied last — where a list's
    /// generator switches off the very rule the list feeds, to see what the user
    /// would get without it.
    pub(crate) fn engine(self, adjust: impl FnOnce(&mut EngineConfig)) -> Engine {
        let mut engine = Engine::new();
        engine.update_config(|config| {
            config.method = self.method;
            config.syllable_rules = self.rules;
            config.eager_restore = self.eager;
            adjust(config);
        });
        engine
    }
}

/// Every setup a user can type English under where the engine could differ: both
/// letter-keyed methods, with and without the extra onsets that let `f`/`j`/`w`/`z`
/// start a syllable (`ferry` only loses an `r` once `f` is an onset), and with eager
/// restore on and off.
pub(crate) const SETUPS: [Setup; 8] = [
    Setup::new(InputMethod::Telex, SyllableRules::STANDARD, true),
    Setup::new(InputMethod::Telex, WIDE, true),
    Setup::new(InputMethod::TelexAdvanced, SyllableRules::STANDARD, true),
    Setup::new(InputMethod::TelexAdvanced, WIDE, true),
    Setup::new(InputMethod::Telex, SyllableRules::STANDARD, false),
    Setup::new(InputMethod::Telex, WIDE, false),
    Setup::new(InputMethod::TelexAdvanced, SyllableRules::STANDARD, false),
    Setup::new(InputMethod::TelexAdvanced, WIDE, false),
];

const WIDE: SyllableRules = SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);
