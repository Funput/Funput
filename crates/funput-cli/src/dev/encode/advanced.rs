use funput_core::InputMethod;

use super::Shape;

/// Emit a Full Telex vowel shortcut, returning whether it replaced the base vowel.
pub(super) fn push_shortcut(
    method: InputMethod,
    index: usize,
    base: char,
    shape: Option<Shape>,
    out: &mut String,
) -> bool {
    if !method.is_advanced_telex() || !matches!(shape, Some(Shape::Horn)) {
        return false;
    }
    let shortcut = match (base, index) {
        ('u', 0) => 'w',
        ('U', 0) => 'W',
        ('u', _) => '[',
        ('o', _) => ']',
        // Full Telex deliberately has no `{`/`}` uppercase shortcuts.
        _ => return false,
    };
    out.push(shortcut);
    true
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn only_advanced_horn_vowels_use_shortcuts() {
        let mut output = String::new();
        assert!(push_shortcut(
            InputMethod::TelexAdvanced,
            1,
            'u',
            Some(Shape::Horn),
            &mut output
        ));
        assert_eq!(output, "[");
        assert!(!push_shortcut(
            InputMethod::Telex,
            1,
            'o',
            Some(Shape::Horn),
            &mut output
        ));
    }
}
