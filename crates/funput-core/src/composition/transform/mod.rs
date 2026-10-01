//! Transform pipeline: classify a key, then resolve the selected action.

mod action;
mod full_telex;
mod gates;
mod normal;

use crate::input_method::{AdvancedAction, telex, vni};
use crate::{ComposeOptions, InputMethod, TransformResult};

pub(crate) use action::apply_action;

/// Classify `key` with the method's grammar, then resolve what it asks for.
pub(crate) fn apply(buffer: &str, key: char, options: ComposeOptions) -> TransformResult {
    let action = match options.method {
        InputMethod::Telex => telex::classify_key(buffer, key),
        InputMethod::Vni => vni::classify_key(buffer, key),
        InputMethod::TelexAdvanced => match telex::classify_advanced_key(buffer, key) {
            AdvancedAction::Standard(action) => action,
            AdvancedAction::Shortcut(shortcut) => {
                return full_telex::apply(buffer, key, shortcut, options);
            }
        },
    };
    apply_action(buffer, key, action, options)
}

pub(super) fn append(buffer: &str, key: char) -> String {
    let mut text = String::with_capacity(buffer.len() + key.len_utf8());
    text.push_str(buffer);
    text.push(key);
    text
}

#[cfg(test)]
mod tests;
