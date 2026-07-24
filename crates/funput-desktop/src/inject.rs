//! The inject plan: what a "hook + inject" shell emits for an engine result.

use funput_engine::{Action, ImeResult};

/// What to emit to the focused app for an [`ImeResult`]: delete `backspaces`
/// characters, then type `units` (the UTF-16 code units of the composed output).
///
/// UTF-16 because that is what Windows `SendInput` (`KEYEVENTF_UNICODE`) consumes;
/// Vietnamese NFC stays in the BMP (one unit per char), but surrogate pairs are
/// handled correctly for any other text.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct InjectPlan {
    /// Number of preceding characters to delete (Backspace presses).
    pub backspaces: usize,
    /// UTF-16 code units to type after the deletions.
    pub units: Vec<u16>,
}

impl InjectPlan {
    /// Nothing to inject — the key should pass through to the app unchanged.
    pub fn is_noop(&self) -> bool {
        self.backspaces == 0 && self.units.is_empty()
    }
}

/// Translate an engine result into an [`InjectPlan`].
///
/// - [`Action::None`] → empty plan: let the key reach the app untouched.
/// - [`Action::Send`] / [`Action::Restore`] → delete `backspace` chars, then type
///   `output`. The triggering key is swallowed by the shell.
pub fn plan_inject(result: &ImeResult) -> InjectPlan {
    match result.action {
        Action::None => InjectPlan::default(),
        Action::Send | Action::Restore => InjectPlan {
            backspaces: result.backspace,
            units: result.output.encode_utf16().collect(),
        },
    }
}

#[cfg(test)]
mod tests {
    use funput_engine::{Action, ImeResult};

    use super::*;

    #[test]
    fn plan_none_is_noop() {
        let plan = plan_inject(&ImeResult {
            action: Action::None,
            backspace: 0,
            output: String::new(),
        });
        assert!(plan.is_noop());
    }

    #[test]
    fn plan_send_deletes_then_types_utf16() {
        let plan = plan_inject(&ImeResult {
            action: Action::Send,
            backspace: 1,
            output: "á".into(),
        });
        assert_eq!(plan.backspaces, 1);
        assert_eq!(plan.units, "á".encode_utf16().collect::<Vec<_>>());
        assert_eq!(plan.units.len(), 1); // BMP: one unit
    }

    #[test]
    fn plan_restore_word() {
        let plan = plan_inject(&ImeResult {
            action: Action::Restore,
            backspace: 3,
            output: "card ".into(),
        });
        assert_eq!(plan.backspaces, 3);
        assert_eq!(plan.units, "card ".encode_utf16().collect::<Vec<_>>());
    }
}
