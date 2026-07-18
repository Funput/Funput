//! Reverse of compose: turn finished Vietnamese text into the Telex/VNI keystrokes
//! that would produce it. Self-contained (Unicode NFD), used by the coverage check
//! to round-trip a corpus: `text → encode → engine → text`.

mod advanced;

use unicode_normalization::UnicodeNormalization;

use funput_core::InputMethod;

#[derive(Clone, Copy)]
enum Shape {
    Circumflex, // â ê ô
    Breve,      // ă
    Horn,       // ơ ư
}

#[derive(Clone, Copy)]
enum Tone {
    Grave, // huyền
    Acute, // sắc
    Hook,  // hỏi
    Tilde, // ngã
    Dot,   // nặng
}

/// Encode finished Vietnamese `text` into the keystrokes for `method`.
pub fn encode(text: &str, method: InputMethod) -> String {
    let mut out = String::new();
    let chars: Vec<char> = text.chars().collect();
    for (i, &ch) in chars.iter().enumerate() {
        // Telex only: a genuine double `o` (`boong`, `soóc`, `xoong`) collides with
        // the `oo`→`ô` digraph. Typing a third `o` escapes it (`booong`→`boong`), so
        // when a plain `o` (bare `o`, any tone — but not `ô`/`ơ`) immediately follows
        // another plain `o`, emit the extra escape key before encoding it.
        if method.is_telex_family() && i > 0 && is_plain_o(ch) && is_plain_o(chars[i - 1]) {
            out.push(if ch.is_uppercase() { 'O' } else { 'o' });
        }
        encode_char(ch, i, method, &mut out);
    }
    out
}

/// A bare `o`/`O` carrying at most a tone — excludes `ô` (circumflex) and `ơ` (horn),
/// which are distinct vowels Telex types differently.
fn is_plain_o(ch: char) -> bool {
    let mut marks = ch.nfd();
    matches!(marks.next(), Some('o') | Some('O'))
        && !marks.any(|m| matches!(m, '\u{0302}' | '\u{031B}'))
}

fn encode_char(ch: char, index: usize, method: InputMethod, out: &mut String) {
    // `đ`/`Đ` do not decompose under NFD — handle the stroke explicitly.
    match ch {
        'đ' => return push_stroke(method, 'd', out),
        'Đ' => return push_stroke(method, 'D', out),
        _ => {}
    }

    let marks: Vec<char> = ch.nfd().collect();
    let base = marks[0];

    let mut shape = None;
    let mut tone = None;
    for &m in &marks[1..] {
        match m {
            '\u{0302}' => shape = Some(Shape::Circumflex),
            '\u{0306}' => shape = Some(Shape::Breve),
            '\u{031B}' => shape = Some(Shape::Horn),
            '\u{0300}' => tone = Some(Tone::Grave),
            '\u{0301}' => tone = Some(Tone::Acute),
            '\u{0309}' => tone = Some(Tone::Hook),
            '\u{0303}' => tone = Some(Tone::Tilde),
            '\u{0323}' => tone = Some(Tone::Dot),
            _ => {}
        }
    }

    // Full Telex shortcuts replace both the base vowel and its shape key.
    if !advanced::push_shortcut(method, index, base, shape, out) {
        out.push(base);
        if let Some(s) = shape {
            push_shape(method, base, s, out);
        }
    }
    if let Some(t) = tone
        && let Some(key) = tone_key(method, t)
    {
        out.push(key);
    }
}

/// `đ`: Telex doubles the `d` (`dd`/`Dd`); VNI uses the `9` modifier (`d9`/`D9`).
fn push_stroke(method: InputMethod, d: char, out: &mut String) {
    out.push(d);
    match method {
        InputMethod::Telex | InputMethod::TelexAdvanced => out.push('d'),
        InputMethod::Vni => out.push('9'),
        _ => {}
    }
}

fn push_shape(method: InputMethod, base: char, shape: Shape, out: &mut String) {
    match method {
        // Telex: circumflex doubles the vowel (`aa`→â); breve/horn use `w`.
        InputMethod::Telex | InputMethod::TelexAdvanced => match shape {
            Shape::Circumflex => out.push(base.to_ascii_lowercase()),
            Shape::Breve | Shape::Horn => out.push('w'),
        },
        // VNI: 6 = circumflex, 8 = breve, 7 = horn.
        InputMethod::Vni => out.push(match shape {
            Shape::Circumflex => '6',
            Shape::Breve => '8',
            Shape::Horn => '7',
        }),
        _ => {}
    }
}

fn tone_key(method: InputMethod, tone: Tone) -> Option<char> {
    match method {
        InputMethod::Telex | InputMethod::TelexAdvanced => Some(match tone {
            Tone::Acute => 's',
            Tone::Grave => 'f',
            Tone::Hook => 'r',
            Tone::Tilde => 'x',
            Tone::Dot => 'j',
        }),
        InputMethod::Vni => Some(match tone {
            Tone::Acute => '1',
            Tone::Grave => '2',
            Tone::Hook => '3',
            Tone::Tilde => '4',
            Tone::Dot => '5',
        }),
        _ => None,
    }
}

#[cfg(test)]
mod tests;
