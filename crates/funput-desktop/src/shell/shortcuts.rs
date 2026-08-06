//! The gõ tắt (text expansion) table.
//!
//! The Settings UI edits rows in place, which means a row is half-typed for as
//! long as the user is typing it. Those drafts have to stay in the list the UI
//! renders, but must never reach the engine or the disk — an expansion with an
//! empty trigger would fire on every word boundary. So each edit persists the
//! complete rows only and puts the drafts straight back.

use funput_config::Shortcut;

use super::ShellState;
use super::config::push_shortcuts;

/// A row the engine and disk will keep — both sides filled (whitespace ignored).
fn is_complete(sc: &Shortcut) -> bool {
    !sc.trigger.trim().is_empty() && !sc.expansion.trim().is_empty()
}

impl ShellState {
    /// Persist and push the complete rows, keeping drafts in memory for the UI.
    fn commit_shortcuts(&mut self) {
        let complete: Vec<Shortcut> = self
            .settings
            .shortcuts
            .iter()
            .filter(|sc| is_complete(sc))
            .cloned()
            .collect();
        push_shortcuts(&mut self.engine, &complete);
        let drafts = std::mem::replace(&mut self.settings.shortcuts, complete);
        self.save();
        self.settings.shortcuts = drafts;
    }

    /// "Thêm" is allowed when the list is empty or the last row is already
    /// complete — otherwise a second blank row would appear above the first.
    pub fn can_add_shortcut(&self) -> bool {
        self.settings.shortcuts.last().is_none_or(is_complete)
    }

    pub fn add_shortcut(&mut self) {
        if !self.can_add_shortcut() {
            return;
        }
        self.settings.shortcuts.push(Shortcut {
            trigger: String::new(),
            expansion: String::new(),
        });
        self.commit_shortcuts();
    }

    /// Drop blank / half-filled drafts (e.g. when leaving the Gõ tắt page).
    pub fn prune_incomplete_shortcuts(&mut self) {
        let before = self.settings.shortcuts.len();
        self.settings.shortcuts.retain(is_complete);
        if self.settings.shortcuts.len() != before {
            self.commit_shortcuts();
        }
    }

    pub fn remove_shortcut(&mut self, index: usize) {
        if index < self.settings.shortcuts.len() {
            self.settings.shortcuts.remove(index);
            self.commit_shortcuts();
        }
    }

    pub fn set_shortcut_trigger(&mut self, index: usize, trigger: String) {
        if let Some(sc) = self.settings.shortcuts.get_mut(index) {
            sc.trigger = trigger;
            self.commit_shortcuts();
        }
    }

    pub fn set_shortcut_expansion(&mut self, index: usize, expansion: String) {
        if let Some(sc) = self.settings.shortcuts.get_mut(index) {
            sc.expansion = expansion;
            self.commit_shortcuts();
        }
    }
}
