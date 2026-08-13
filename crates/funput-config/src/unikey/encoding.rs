//! Turning a macro file's bytes into text.
//!
//! Reading bytes rather than `fs::read_to_string` is deliberate: that helper hard-
//! fails on anything that is not UTF-8 and cannot tell "this is UTF-16" from "this
//! is corrupt". A legacy 8-bit Vietnamese encoding (TCVN3, VNI-Windows) still fails
//! here, but as its own answer, so the caller can say what to do about it — guessing
//! at one of those would silently import mojibake the user then has to hunt down row
//! by row.

/// Decode by byte-order mark, falling back to plain UTF-8. `None` when the bytes are
/// none of those.
pub(super) fn decode(bytes: &[u8]) -> Option<String> {
    if let Some(rest) = bytes.strip_prefix(&[0xEF, 0xBB, 0xBF]) {
        return String::from_utf8(rest.to_vec()).ok();
    }
    if let Some(rest) = bytes.strip_prefix(&[0xFF, 0xFE]) {
        return utf16(rest, u16::from_le_bytes);
    }
    if let Some(rest) = bytes.strip_prefix(&[0xFE, 0xFF]) {
        return utf16(rest, u16::from_be_bytes);
    }
    String::from_utf8(bytes.to_vec()).ok()
}

fn utf16(bytes: &[u8], unit: fn([u8; 2]) -> u16) -> Option<String> {
    if !bytes.len().is_multiple_of(2) {
        return None;
    }
    let units: Vec<u16> = bytes
        .chunks_exact(2)
        .map(|pair| unit([pair[0], pair[1]]))
        .collect();
    String::from_utf16(&units).ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_utf8_bom_is_stripped_rather_than_decoded_into_the_text() {
        assert_eq!(decode(b"\xEF\xBB\xBFvn").as_deref(), Some("vn"));
    }

    #[test]
    fn plain_utf8_without_a_bom_still_decodes() {
        assert_eq!(
            decode("vn:việt nam".as_bytes()).as_deref(),
            Some("vn:việt nam")
        );
    }

    #[test]
    fn both_utf16_byte_orders_decode() {
        let text = "vn:việt nam";
        let mut le = vec![0xFF, 0xFE];
        let mut be = vec![0xFE, 0xFF];
        for unit in text.encode_utf16() {
            le.extend_from_slice(&unit.to_le_bytes());
            be.extend_from_slice(&unit.to_be_bytes());
        }
        assert_eq!(decode(&le).as_deref(), Some(text));
        assert_eq!(decode(&be).as_deref(), Some(text));
    }

    #[test]
    fn a_truncated_utf16_file_is_refused_rather_than_half_read() {
        assert_eq!(decode(&[0xFF, 0xFE, 0x76]), None);
    }

    #[test]
    fn a_legacy_eight_bit_encoding_is_refused() {
        // 0xFF mid-stream: not valid UTF-8, no BOM to explain it.
        assert_eq!(decode(b"vn:vi\xFFt nam"), None);
    }
}
