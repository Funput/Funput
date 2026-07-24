//! Pure translation of an engine [`ImeResult`] into bytes for the child PTY.

use funput_engine::{Action, ImeResult};

/// DEL — what a line editor treats as "delete previous character".
const DEL: u8 = 0x7f;

/// Bytes to write to the child after composing `key`.
///
/// - [`Action::None`]: the key passes through as its UTF-8 bytes.
/// - [`Action::Send`] / [`Action::Restore`]: delete `backspace` chars (DEL), then
///   inject the composed `output`.
pub fn result_bytes(key: char, result: &ImeResult) -> Vec<u8> {
    match result.action {
        Action::None => {
            let mut buf = [0u8; 4];
            key.encode_utf8(&mut buf).as_bytes().to_vec()
        }
        Action::Send | Action::Restore => {
            let mut bytes = vec![DEL; result.backspace];
            bytes.extend_from_slice(result.output.as_bytes());
            bytes
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn none_passes_key_through() {
        let r = ImeResult {
            action: Action::None,
            backspace: 0,
            output: String::new(),
        };
        assert_eq!(result_bytes('a', &r), b"a".to_vec());
    }

    #[test]
    fn send_deletes_then_injects() {
        let r = ImeResult {
            action: Action::Send,
            backspace: 1,
            output: "á".into(),
        };
        let mut expected = vec![DEL];
        expected.extend_from_slice("á".as_bytes());
        assert_eq!(result_bytes('s', &r), expected);
    }

    #[test]
    fn restore_deletes_whole_word() {
        let r = ImeResult {
            action: Action::Restore,
            backspace: 3,
            output: "card ".into(),
        };
        let bytes = result_bytes(' ', &r);
        assert_eq!(&bytes[..3], &[DEL, DEL, DEL]);
        assert_eq!(&bytes[3..], "card ".as_bytes());
    }
}
