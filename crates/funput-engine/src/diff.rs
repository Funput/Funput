//! Buffer diff → backspace count + output suffix for platform inject.

/// Byte length of the longest common char prefix of `old` and `new`.
pub(crate) fn common_prefix_bytes(old: &str, new: &str) -> usize {
    let mut prefix = 0;
    for (o, n) in old.chars().zip(new.chars()) {
        if o != n {
            break;
        }
        prefix += o.len_utf8();
    }
    prefix
}

/// Compare composed text before and after a transform.
///
/// Returns `(backspace, output)` where the platform deletes `backspace` chars
/// then injects `output`. Allocates only the output suffix.
pub(crate) fn diff(old: &str, new: &str) -> (usize, String) {
    let prefix = common_prefix_bytes(old, new);
    let backspace = old[prefix..].chars().count();
    (backspace, new[prefix..].to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn diff_single_vowel_tone() {
        assert_eq!(diff("a", "á"), (1, "á".to_string()));
    }

    #[test]
    fn diff_reposition_suffix() {
        assert_eq!(diff("hoa", "hoà"), (1, "à".to_string()));
    }

    #[test]
    fn diff_revert_shape() {
        assert_eq!(diff("â", "a"), (1, "a".to_string()));
    }

    #[test]
    fn diff_unchanged() {
        assert_eq!(diff("x", "x"), (0, String::new()));
    }

    #[test]
    fn common_prefix_is_char_aligned() {
        // `â` vs `á` share a UTF-8 lead byte but differ as chars — the prefix
        // must stay on the char boundary before them.
        assert_eq!(common_prefix_bytes("â", "á"), 0);
        assert_eq!(common_prefix_bytes("viê", "việ"), "vi".len());
        assert_eq!(common_prefix_bytes("ab", "abc"), 2);
    }
}
