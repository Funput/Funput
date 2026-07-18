use funput_core::InputMethod;

use super::*;

mod behavior;
mod config;

fn config_for(method: InputMethod) -> TermConfig {
    TermConfig {
        method,
        ..TermConfig::default()
    }
}

fn compose(method: InputMethod, input: &[u8]) -> Vec<u8> {
    let state = SharedState::new(true);
    let mut out = Vec::new();
    forward_input(input, &mut out, &config_for(method), &state, |_| {}).unwrap();
    out
}

fn reconstruct(bytes: &[u8]) -> String {
    let mut text = String::new();
    let mut index = 0;
    while index < bytes.len() {
        let byte = bytes[index];
        if byte == 0x7f {
            text.pop();
            index += 1;
            continue;
        }
        let len = match byte {
            _ if byte < 0x80 => 1,
            _ if byte >> 5 == 0b110 => 2,
            _ if byte >> 4 == 0b1110 => 3,
            _ => 4,
        };
        if let Ok(value) = std::str::from_utf8(&bytes[index..index + len]) {
            text.push_str(value);
        }
        index += len;
    }
    text
}
