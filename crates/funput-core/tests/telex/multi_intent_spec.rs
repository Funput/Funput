use funput_core::{InputMethod, ToneStyle, apply};

struct Case {
    skeleton: &'static str,
    target: &'static str,
}

fn type_keys(keys: &str, style: ToneStyle) -> String {
    keys.chars().fold(String::new(), |buffer, key| {
        apply(&buffer, key, InputMethod::Telex, style).text
    })
}

fn insert_pair(base: &str, d_at: usize, w_at: usize, w_first: bool) -> String {
    let chars: Vec<_> = base.chars().collect();
    let mut keys = String::with_capacity(base.len() + 2);
    for boundary in 0..=chars.len() {
        if d_at == boundary && w_at == boundary {
            if w_first {
                keys.push('w');
                keys.push('d');
            } else {
                keys.push('d');
                keys.push('w');
            }
        } else {
            if d_at == boundary {
                keys.push('d');
            }
            if w_at == boundary {
                keys.push('w');
            }
        }
        if let Some(ch) = chars.get(boundary) {
            keys.push(*ch);
        }
    }
    keys
}

fn variants(base: &str) -> Vec<String> {
    let count = base.chars().count();
    let mut result = Vec::new();
    for d_at in 1..=count {
        for w_at in 1..=count {
            result.push(insert_pair(base, d_at, w_at, false));
            if d_at == w_at {
                result.push(insert_pair(base, d_at, w_at, true));
            }
        }
    }
    result
}

#[test]
fn all_bounded_multi_intent_orders_converge() {
    for case in [
        Case {
            skeleton: "duocj",
            target: "được",
        },
        Case {
            skeleton: "duongf",
            target: "đường",
        },
        Case {
            skeleton: "don",
            target: "đơn",
        },
        Case {
            skeleton: "doif",
            target: "đời",
        },
    ] {
        for style in [ToneStyle::Traditional, ToneStyle::Modern] {
            for keys in variants(case.skeleton) {
                assert_eq!(
                    type_keys(&keys, style),
                    case.target,
                    "did not converge: {keys}"
                );
            }
        }
    }
}

#[test]
fn tones_and_uppercase_converge() {
    for (skeleton, target) in [
        ("dans", "đắn"),
        ("danf", "đằn"),
        ("danr", "đẳn"),
        ("danx", "đẵn"),
        ("danj", "đặn"),
        ("DUOCJ", "ĐƯỢC"),
    ] {
        for keys in variants(skeleton) {
            assert_eq!(type_keys(&keys, ToneStyle::Traditional), target, "{keys}");
        }
    }
}
