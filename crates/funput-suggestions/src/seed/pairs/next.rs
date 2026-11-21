//! A word that follows a shipped first word, in both tone styles.

use std::borrow::Cow;

use funput_core::{ToneStyle, respell_tone};

#[derive(Clone)]
pub(crate) struct Next {
    modern: &'static str,
    traditional: Cow<'static, str>,
}

impl Next {
    pub(super) fn new(modern: &'static str) -> Self {
        let traditional =
            respell_tone(modern, ToneStyle::Traditional).map_or(Cow::Borrowed(modern), Cow::Owned);
        Self {
            modern,
            traditional,
        }
    }

    /// The spelling the bar writes for a user who places tones in `style`.
    pub(crate) fn spelled(&self, style: ToneStyle) -> &str {
        match style {
            ToneStyle::Modern => self.modern,
            ToneStyle::Traditional => &self.traditional,
        }
    }

    /// Both spellings, the same one twice when the styles agree. A word the user
    /// blocked in one style stays blocked in the other.
    pub(crate) fn spellings(&self) -> impl Iterator<Item = &str> {
        [self.modern, &*self.traditional].into_iter()
    }

    /// What this word holds on the heap: its traditional spelling, when the
    /// styles disagree.
    #[cfg(test)]
    pub(super) fn heap_bytes(&self) -> usize {
        match &self.traditional {
            Cow::Owned(text) => text.capacity(),
            Cow::Borrowed(_) => 0,
        }
    }
}
