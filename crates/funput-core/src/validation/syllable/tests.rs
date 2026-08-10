use super::*;
use crate::validation::coda::{MAX_CODA, normalized_coda, toneless_rhyme};
use crate::validation::reachability::is_definitely_invalid;

#[test]
fn validate_tone_cases() {
    assert_eq!(validate_tone("ng"), ModifierValidation::Ignored);
    assert_eq!(validate_tone("text"), ModifierValidation::PassThrough);
    assert_eq!(validate_tone("mix"), ModifierValidation::Allow);
    assert_eq!(validate_tone("ma"), ModifierValidation::Allow);
    assert_eq!(validate_tone("zt"), ModifierValidation::PassThrough);
}

#[test]
fn validate_stroke_cases() {
    assert_eq!(validate_stroke("d"), ModifierValidation::Allow);
    // A d anywhere in the buffer allows the stroke: dang9 → đang, GD9 → GĐ.
    assert_eq!(validate_stroke("dang"), ModifierValidation::Allow);
    assert_eq!(validate_stroke("GD"), ModifierValidation::Allow);
    assert_eq!(validate_stroke("x"), ModifierValidation::Ignored);
    assert_eq!(validate_stroke("bag"), ModifierValidation::Ignored);
}

#[test]
fn is_valid_cases() {
    assert!(is_valid("má"));
    assert!(is_valid("ma"));
    assert!(!is_valid("ábc"));
    assert!(!is_valid("text"));
}

#[test]
fn is_complete_syllable_cases() {
    // Complete Vietnamese syllables. `k` + `y` (kỳ/ký/kỹ) and the triphthong
    // `ngoài` are regression guards for tone-placement / ckg-spelling fixes.
    for ok in [
        "má",
        "ma",
        "tét",
        "việt",
        "trường",
        "quá",
        "ăn",
        "nhanh",
        "kỳ",
        "ký",
        "kỹ",
        "ngoài",
    ] {
        assert!(is_complete_syllable(ok), "{ok} should be complete");
    }
    // Invalid finals — a finished word ending in a non-Vietnamese coda.
    for bad in ["cảd", "côl", "máz", "hảd", "ng", "abc", "text"] {
        assert!(!is_complete_syllable(bad), "{bad} should be incomplete");
    }
    // Stricter than `is_valid`: single trailing `d`/`z` is lenient-valid but
    // not a complete syllable.
    assert!(is_valid("cảd"));
    assert!(!is_complete_syllable("cảd"));
}

#[test]
fn real_syllables_are_complete() {
    // Broad battery of real Vietnamese syllables (incl. hard rhymes). A failure
    // means the rhyme table is missing an entry — add it to `rhyme.rs`.
    let words = "\
        a ba cá chè dê đi em gà gh ghê gì hoa khô là mẹ nó ô phở quà rể sữa tô \
        uô(no) việt nghĩa người trường nước được rượu hươu khuya khuỷu quýnh quyên \
        nguyệt khuếch doanh hoạch bâng khuâng ngoằn ngoèo tuềnh toàng xoèn xoẹt \
        muốn muống thuốc nhuộm tuốt cướp lướt mượn đường riêng tiếng chuông \
        anh ánh ách inh tính kịch lệnh xanh sạch huỳnh quỳnh xoong(no) \
        hoàng khoảng nguyên nguyền quyết tuyết duyên xuân xuất bâng khuâng \
        ngoắt ngoéo ngoạm ngoạp ngoạc ngoắc loắt choắt \
        cằn nhằn lẳng lặng phưng phức nưng nửng \
        tay hai cao sau cau mây đây kẹo kêu cừu mưu líu xíu";
    for w in words.split_whitespace() {
        if w.ends_with("(no)") {
            continue;
        }
        // skip onset-only fragments used as spacers
        if w == "gh" {
            continue;
        }
        assert!(
            is_complete_syllable(w),
            "{w} (rhyme {:?}) should be a complete syllable",
            {
                let p = parse_syllable(w);
                let (coda, len) = normalized_coda(&p).unwrap_or((['\0'; MAX_CODA], 0));
                toneless_rhyme(&p, &coda[..len])
            }
        );
    }
}

#[test]
fn definitely_invalid_detects_dead_ends() {
    // Dead ends — unreachable rhyme (incl. open clusters), or stop coda +
    // wrong (huyền/hỏi/ngã) tone.
    for dead in ["tẽt", "tèt", "cảd", "máz", "pèect", "ábc", "caé", "luuỷ"] {
        assert!(is_definitely_invalid(dead), "{dead} should be a dead end");
    }
    // Alive: still typing, already valid, OR a stop coda awaiting its tone
    // (`nuoc`/`nươc`/`côt` → user types the tone after the coda).
    for alive in [
        "tẽ", "te", "ng", "ngh", "cả", "cản", "việt", "má", "trươ", "nuoc", "nươc", "côt", "tét",
    ] {
        assert!(!is_definitely_invalid(alive), "{alive} should stay alive");
    }
}

#[test]
fn stop_coda_only_allows_sac_or_nang() {
    // Legal: stop coda with sắc or nặng.
    for ok in ["tét", "tẹt", "sách", "học", "đẹp", "việt", "nước"] {
        assert!(is_complete_syllable(ok), "{ok} should be complete");
    }
    // Illegal: stop coda with ngang / huyền / hỏi / ngã — the signal that
    // catches English words like `text` (→ `tẽt`) or `coot` (→ `côt`).
    for bad in ["tẽt", "tèt", "tẻt", "côt", "sàch", "mãc"] {
        assert!(!is_complete_syllable(bad), "{bad} should be incomplete");
    }
    // Sonorant codas keep all tones legal.
    for ok in ["làng", "mển", "ngã", "cũng"] {
        assert!(is_complete_syllable(ok), "{ok} should be complete");
    }
}

#[test]
fn reopenable_accepts_a_stop_coda_awaiting_its_tone() {
    // The words this exists for: committed without a tone, re-opened so the next
    // tone key finishes them (`chuc` + `s` → `chúc`).
    for ok in [
        "chuc", "tich", "hoc", "dat", "viêt", "nươc", "chào", "phủ", "ma",
    ] {
        assert!(is_reopenable_syllable(ok), "{ok} should be re-openable");
    }
    // A wrong tone on a stop coda, or no syllable at all, stays refused — those are
    // the English words the gate is there to protect.
    for bad in ["", "tẽt", "tèt", "côl", "cảd", "text", "hello", "chào bạn"] {
        assert!(!is_reopenable_syllable(bad), "{bad} should be refused");
    }
    // Strictly looser than `is_complete_syllable`, never the other way round.
    for w in ["chuc", "tét", "tẽt", "ma", "text"] {
        assert!(!is_complete_syllable(w) || is_reopenable_syllable(w));
    }
}

#[test]
fn ckg_spelling() {
    assert_eq!(validate_tone("ke"), ModifierValidation::Allow);
    // `k` is exempt from the pairing rule for loanwords/toponyms (Kông, Kenya).
    assert_eq!(validate_tone("ka"), ModifierValidation::Allow);
    assert_eq!(validate_tone("ca"), ModifierValidation::Allow);
    // `c`+front still needs `k`; `ge` would need `gh` — these stay restricted.
    assert_eq!(validate_tone("ce"), ModifierValidation::PassThrough);
    assert_eq!(validate_tone("ge"), ModifierValidation::PassThrough);
    // `gi` digraph stays valid.
    assert_eq!(validate_tone("gi"), ModifierValidation::Allow);
}
