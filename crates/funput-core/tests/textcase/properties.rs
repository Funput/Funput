//! Properties that must hold for text no fixture would think to write down —
//! arbitrary Unicode, lone combining marks, scripts with no case at all.

use funput_core::textcase::{Options, Transform, apply};
use proptest::prelude::*;

/// Every transform there is. A wildcard is not available here — `Transform` is
/// exhaustive on purpose — so a new variant makes this array a compile error until
/// somebody decides which properties it has to satisfy.
const ALL: [Transform; 3] = [Transform::Upper, Transform::Lower, Transform::NoDiacritics];

/// Vietnamese as it is actually stored: precomposed letters, the stroke, the eight
/// combining marks, and the punctuation that decides a sentence boundary. A plain
/// `any::<String>()` would almost never produce a toned vowel, so a property run
/// over it would be checking nothing about Vietnamese.
const VIETNAMESE: &str = "[a-zA-Z0-9 .!?\\n\
    aăâeêioôơuưyđAĂÂEÊIOÔƠUƯYĐ\
    áàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ\
    ÁÀẢÃẠẾỆỐỚỮ\
    \\x{300}\\x{301}\\x{303}\\x{309}\\x{323}\\x{302}\\x{306}\\x{31B}]{0,48}";

proptest! {
    /// No transform panics, and none of them grows the text. Case mapping is
    /// allowed to add characters in general (`ß` → `SS`), but no Vietnamese letter
    /// does, and bỏ dấu only ever removes.
    #[test]
    fn never_panics_and_never_grows(text in VIETNAMESE) {
        for transform in ALL {
            let out = apply(&text, transform, Options::default());
            prop_assert!(
                out.chars().count() <= text.chars().count(),
                "{transform:?} grew {text:?} into {out:?}"
            );
        }
    }

    /// Arbitrary Unicode — emoji, unassigned code points, lone marks, right-to-left
    /// text — reaches the same code and must not panic there either.
    #[test]
    fn arbitrary_unicode_is_survivable(text in any::<String>()) {
        for transform in ALL {
            let _ = apply(&text, transform, Options::default());
        }
    }

    /// Running a transform on its own output changes nothing. The one that would be
    /// easy to get wrong is bỏ dấu in the combining form: drop the mark but leave
    /// the base letter respelled, and a second pass would keep finding work.
    #[test]
    fn every_transform_is_idempotent(text in VIETNAMESE) {
        for transform in ALL {
            let once = apply(&text, transform, Options::default());
            prop_assert_eq!(
                apply(&once, transform, Options::default()),
                once.clone(),
                "{:?} is not idempotent on {:?}",
                transform,
                text
            );
        }
    }

    /// Vietnamese in, ASCII out — the property the whole transform exists for, and
    /// the one a `family → ASCII` table would have been at risk of missing a row of.
    #[test]
    fn bo_dau_leaves_only_ascii(text in VIETNAMESE) {
        let bare = apply(&text, Transform::NoDiacritics, Options::default());
        prop_assert!(bare.is_ascii(), "{text:?} left {bare:?}");
    }

    /// With the switch off, the stroke is the *only* thing that may survive.
    #[test]
    fn keeping_the_stroke_keeps_nothing_else(text in VIETNAMESE) {
        let options = Options {
            d_to_ascii: false,
            ..Options::default()
        };
        let bare = apply(&text, Transform::NoDiacritics, options);
        prop_assert!(
            bare.chars().all(|c| c.is_ascii() || c == 'đ' || c == 'Đ'),
            "{text:?} left {bare:?}"
        );
    }
}
