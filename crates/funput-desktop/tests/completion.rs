//! Enter over an app's completion list (#712): in Mattermost, `@pcod` then Enter picks
//! a person, it does not break a line, so the next word must not be capitalized. The
//! same for an entry chosen with Up or Down first, and for `#`, `:` and `~`. Driven
//! through the real `ShellState`, the way the Windows hook drives it.

use funput_desktop::{Caret, ShellState, TriggerKey};
use funput_engine::{Action, KeySource};

/// A shell with auto-capitalize on, and the text the app shows.
struct Chat {
    state: ShellState,
    app: String,
}

impl Chat {
    fn new() -> Self {
        let mut state = ShellState::new(None);
        state.set_auto_capitalize(true);
        Self {
            state,
            app: String::new(),
        }
    }

    /// One row, `ct` → `cơm tấm`, expanding on Enter.
    fn with_shortcut_on_enter() -> Self {
        let mut chat = Self::new();
        chat.state.add_shortcut();
        chat.state.set_shortcut_trigger(0, "ct".into());
        chat.state.set_shortcut_expansion(0, "cơm tấm".into());
        chat.state.set_shortcut_on_enter(true);
        chat
    }

    fn type_str(&mut self, keys: &str) -> &mut Self {
        for key in keys.chars() {
            let result = self.state.process_key(key, KeySource::Standard);
            match result.action {
                Action::None => self.app.push(key),
                _ => self.apply(result.backspace, &result.output),
            }
        }
        self
    }

    /// Enter through the hook. The app picks from its list rather than typing a
    /// newline, but what it does is beside the point: what the next word becomes is.
    fn enter(&mut self) -> &mut Self {
        if let Some(expansion) = self.state.trigger_key(TriggerKey::Enter) {
            self.apply(expansion.backspace, &expansion.output);
        }
        self.app.push('\n');
        self
    }

    /// Up or Down through the hook: `classify` says `Flush(Caret::Vertical)`.
    fn arrow(&mut self) -> &mut Self {
        self.state.caret_moved(Caret::Vertical);
        self
    }

    /// The word typed after the last newline.
    fn last_line(&self) -> &str {
        self.app.rsplit('\n').next().unwrap_or_default()
    }

    fn apply(&mut self, backspace: usize, output: &str) {
        for _ in 0..backspace {
            self.app.pop();
        }
        self.app.push_str(output);
    }
}

#[test]
fn enter_that_picks_a_mention_leaves_the_next_word_as_typed() {
    let mut chat = Chat::new();
    chat.type_str("@pcod").enter().type_str("chao");
    assert_eq!(chat.last_line(), "chao");
}

#[test]
fn every_completion_sigil_is_a_list_not_a_line() {
    for word in ["chao @", ":smi", "#gen", "~town"] {
        let mut chat = Chat::new();
        chat.type_str(word).enter().type_str("chao");
        assert_eq!(chat.last_line(), "chao", "after {word}");
    }
}

#[test]
fn an_entry_chosen_with_the_arrows_is_still_picked_by_enter() {
    let mut chat = Chat::new();
    chat.type_str("@pc")
        .arrow()
        .arrow()
        .enter()
        .type_str("chao");
    assert_eq!(chat.last_line(), "chao");
}

/// Typing on, or deleting, edits the name the list completes; it stays open.
#[test]
fn the_list_outlives_letters_typed_after_the_arrows() {
    let mut chat = Chat::new();
    chat.type_str("@pc")
        .arrow()
        .type_str("o")
        .enter()
        .type_str("chao");
    assert_eq!(chat.last_line(), "chao");
}

#[test]
fn a_space_after_the_arrows_closes_the_list() {
    let mut chat = Chat::new();
    chat.type_str("@pc")
        .arrow()
        .type_str(" xong")
        .enter()
        .type_str("chao");
    assert_eq!(chat.last_line(), "Chao");
}

#[test]
fn a_click_after_the_arrows_closes_the_list() {
    let mut chat = Chat::new();
    chat.type_str("@pc").arrow();
    chat.state.caret_moved(Caret::Unknown);
    chat.type_str("xong").enter().type_str("chao");
    assert_eq!(chat.last_line(), "Chao");
}

#[test]
fn a_plain_enter_still_opens_a_sentence() {
    let mut chat = Chat::new();
    chat.type_str("xong").enter().type_str("chao");
    assert_eq!(chat.last_line(), "Chao");
}

#[test]
fn a_sigil_inside_a_word_opens_no_list() {
    let mut chat = Chat::new();
    chat.type_str("a@b.com").enter().type_str("chao");
    assert_eq!(chat.last_line(), "Chao");
}

/// In a multi-line field Up and Down only move the caret: over a word that opens
/// no list they are any caret key, and Enter after them breaks a line.
#[test]
fn the_arrows_over_an_ordinary_word_change_nothing() {
    let mut chat = Chat::new();
    chat.type_str("xong").arrow().enter().type_str("chao");
    assert_eq!(chat.last_line(), "Chao");
}

/// Enter is the list's key there: `@ct` must stay a name, not become `@cơm tấm`.
#[test]
fn enter_over_a_completion_expands_no_shortcut() {
    let mut chat = Chat::with_shortcut_on_enter();
    chat.type_str("@ct").enter();
    assert_eq!(chat.app, "@ct\n");

    let mut chat = Chat::with_shortcut_on_enter();
    chat.type_str("ct").enter();
    assert_eq!(chat.app, "cơm tấm\n");
}
