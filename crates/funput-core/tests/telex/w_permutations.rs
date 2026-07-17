use funput_core::{InputMethod, ToneStyle, apply};

struct Case {
    base: &'static str,
    target: &'static str,
    current: &'static [&'static str],
}

fn insert_w(base: &str, boundary: usize) -> String {
    let offset = base
        .char_indices()
        .nth(boundary)
        .map_or(base.len(), |p| p.0);
    format!("{}w{}", &base[..offset], &base[offset..])
}

fn type_keys(keys: &str, style: ToneStyle) -> String {
    keys.chars().fold(String::new(), |buffer, key| {
        apply(&buffer, key, InputMethod::Telex, style).text
    })
}

fn assert_baseline(case: &Case, style: ToneStyle) {
    let count = case.base.chars().count();
    assert_eq!(case.current.len(), count, "bad snapshot: {}", case.base);
    for boundary in 1..=count {
        let keys = insert_w(case.base, boundary);
        assert_eq!(
            type_keys(&keys, style),
            case.current[boundary - 1],
            "baseline changed for {keys}; V2 target is {}",
            case.target
        );
    }
}

fn assert_converges(case: &Case, style: ToneStyle) {
    for boundary in 1..=case.base.chars().count() {
        let keys = insert_w(case.base, boundary);
        assert_eq!(
            type_keys(&keys, style),
            case.target,
            "did not converge: {keys}"
        );
    }
}

#[test]
fn snapshots_breve_and_single_horn_permutations() {
    for case in [
        Case {
            base: "lams",
            target: "lắm",
            current: &["lwams", "lắm", "lắm", "lắm"],
        },
        Case {
            base: "con",
            target: "cơn",
            current: &["cwon", "cơn", "cơn"],
        },
        Case {
            base: "moif",
            target: "mời",
            current: &["mwòi", "mời", "mời", "mời"],
        },
    ] {
        assert_converges(&case, ToneStyle::Traditional);
    }
}

#[test]
fn snapshots_compound_permutations() {
    for case in [
        Case {
            base: "truongf",
            target: "trường",
            current: &[
                "trừong",
                "trừong",
                "trừong",
                "trường",
                "trường",
                "trường",
                "trưòng",
            ],
        },
        Case {
            base: "nguoif",
            target: "người",
            current: &["ngừoi", "ngừoi", "ngừoi", "người", "người", "ngưòi"],
        },
        Case {
            base: "mua",
            target: "mưa",
            current: &["mưa", "mưa", "mưa"],
        },
        Case {
            base: "uu",
            target: "ưu",
            current: &["ưu", "ưu"],
        },
        Case {
            base: "thuor",
            target: "thuở",
            current: &["thửo", "thửo", "thửo", "thuở", "thửo"],
        },
        Case {
            base: "quois",
            target: "quới",
            current: &["quới", "quói", "quới", "quới", "qưói"],
        },
    ] {
        assert_baseline(&case, ToneStyle::Traditional);
    }
}

#[test]
fn snapshots_all_tones_case_and_styles() {
    for (base, target) in [
        ("lans", "lắn"),
        ("lanf", "lằn"),
        ("lanr", "lẳn"),
        ("lanx", "lẵn"),
        ("lanj", "lặn"),
    ] {
        for style in [ToneStyle::Traditional, ToneStyle::Modern] {
            for boundary in 1..=base.chars().count() {
                assert_eq!(type_keys(&insert_w(base, boundary), style), target);
            }
        }
    }
    assert_eq!(type_keys("LWAMS", ToneStyle::Traditional), "LẮM");
    assert_eq!(type_keys("LAWMS", ToneStyle::Traditional), "LẮM");
}
