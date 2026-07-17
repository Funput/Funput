use crate::unicode::marks::{Tone, apply_tone_to_vowel, tone_on_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, apply_shape_to_vowel};

use super::pair::uo_pair_in_vowel_cluster;

fn horn(ch: char, tone: Option<Tone>) -> Option<char> {
    let shaped = apply_shape_to_vowel(ch, VowelShape::Horn)?;
    Some(match tone {
        Some(tone) => apply_tone_to_vowel(shaped, tone).unwrap_or(shaped),
        None => shaped,
    })
}

pub(crate) fn apply_uo_compound(buffer: &str) -> Option<String> {
    let (u_index, o_index, horn_u) = compound_target(buffer)?;
    let u = buffer.chars().nth(u_index)?;
    let o = buffer.chars().nth(o_index)?;
    let tone = tone_on_vowel(u).or_else(|| tone_on_vowel(o));
    let mut output = String::with_capacity(buffer.len() + 2);
    for (index, ch) in buffer.chars().enumerate() {
        output.push(if index == o_index {
            horn(ch, tone)?
        } else if index == u_index && horn_u {
            horn(ch, None)?
        } else if index == u_index && tone_on_vowel(ch).is_some() {
            vowel_stem(ch)?
        } else {
            ch
        });
    }
    Some(output)
}

pub(crate) fn apply_uo_compound_in_place(text: &mut String) -> bool {
    let buffer = text.as_str();
    let Some((u_index, o_index, horn_u)) = compound_target(buffer) else {
        return false;
    };

    let Some((u_offset, u)) = buffer.char_indices().nth(u_index) else {
        return false;
    };
    let Some((o_offset, o)) = buffer.char_indices().nth(o_index) else {
        return false;
    };
    let tone = tone_on_vowel(u).or_else(|| tone_on_vowel(o));
    let Some(horned_o) = horn(o, tone) else {
        return false;
    };
    text.replace_range(
        o_offset..o_offset + o.len_utf8(),
        horned_o.encode_utf8(&mut [0; 4]),
    );
    if horn_u || tone_on_vowel(u).is_some() {
        let Some(horned_u) = (if horn_u { horn(u, None) } else { vowel_stem(u) }) else {
            return false;
        };
        text.replace_range(
            u_offset..u_offset + u.len_utf8(),
            horned_u.encode_utf8(&mut [0; 4]),
        );
    }
    true
}

fn compound_target(buffer: &str) -> Option<(usize, usize, bool)> {
    let (u_index, o_index) = uo_pair_in_vowel_cluster(buffer)?;
    let continued = o_index + 1 < buffer.chars().count();
    let qu_glide = u_index > 0
        && buffer
            .chars()
            .nth(u_index - 1)
            .is_some_and(|ch| ch.eq_ignore_ascii_case(&'q'));
    Some((u_index, o_index, continued && !qu_glide))
}
