//! What a conversion costs, as numbers and characters — never as a sentence.
//!
//! Two consumers need this and they word it differently: a window names the
//! characters in Vietnamese, a terminal counts them in English. Both are right for
//! where they are, so the wording lives with each of them and only the measurement
//! lives here.

use super::super::{Charset, Conversion, convert};
use super::Pivoted;

/// Everything a conversion lost or respelled, measured.
///
/// **The two failures are separate because they call for different things.** Reading
/// can fail — characters the *source* charset does not define, which means the wrong
/// source was picked or the document is damaged, and the user can act on that.
/// Writing can fail — characters the *target* cannot represent, which is a cost to
/// accept or to avoid by choosing elsewhere. A consumer that reports only the second
/// lets a wrong source guess pass in silence.
#[non_exhaustive]
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Cost {
    /// Characters the **source** charset never defined.
    pub undefined: usize,
    /// The distinct characters the **target** cannot represent, in the order they
    /// first appear. A list rather than a count because a count tells someone
    /// something is wrong without telling them where to look.
    pub lost: Vec<char>,
    /// How many times those characters occur — which is a different number, and the
    /// one a terminal reports. `lost.len()` is what a menu names.
    pub unrepresentable: usize,
    /// Characters the **source** understood exactly but spelled its own way — a
    /// tổ hợp document read as tổ hợp, or an NFD one.
    ///
    /// **Not a loss, and worth keeping apart from one.** The round trip is exact;
    /// folding this into the numbers above would teach people to ignore them. It
    /// counts on the reading side because that is the only side that can see it:
    /// writing always starts from precomposed Unicode, which is spelled one way.
    pub normalized: usize,
}

impl Cost {
    /// True when nothing was lost or guessed at. Respelling does not count.
    pub fn is_clean(&self) -> bool {
        self.undefined == 0 && self.unrepresentable == 0
    }

    pub(super) fn of(pivoted: &Pivoted, to: Charset, out: &Conversion) -> Self {
        Self {
            undefined: pivoted.undefined,
            lost: unrepresentable(&pivoted.text, to),
            unrepresentable: out.unmapped,
            normalized: pivoted.normalized,
        }
    }
}

/// The distinct characters `to` cannot represent, in the order they first appear.
///
/// Asked one character at a time because the counter the driver returns is a total,
/// not a list. Distinct characters in a document are few — a few hundred at most —
/// so this costs a great deal less than it looks like it does.
fn unrepresentable(unicode: &str, to: Charset) -> Vec<char> {
    let mut seen = std::collections::HashSet::new();
    let mut lost = Vec::new();
    for ch in unicode.chars() {
        if !seen.insert(ch) {
            continue;
        }
        if convert(&ch.to_string(), Charset::Unicode, to).unmapped > 0 {
            lost.push(ch);
        }
    }
    lost
}
