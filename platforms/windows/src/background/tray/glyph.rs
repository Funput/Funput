//! The tray glyph, drawn from the logo at the exact size the taskbar shows it.
//!
//! The tray used to ship one 32 px PNG with the F in a 19×24 box inside it, and
//! Windows resampled that bitmap to 16, 20 or 24 px. The F came out about 9×12 px
//! at 100 % next to icons that fill their cell, and soft at every scale (#781).
//! Drawing the vector logo at the pixel size [`super::scale`] reports, cropped to
//! the glyph, fixes both: nothing is resampled and the F fills the cell.

use resvg::tiny_skia::{Pixmap, PremultipliedColorU8, Transform};
use resvg::usvg::{Options, Tree};

/// The colour logo. The mono glyph is its silhouette painted white: the mono SVG
/// separates the strokes with gaps a third of a pixel wide at 16 px, which draw
/// as grey smudges rather than gaps.
const LOGO_SVG: &[u8] = include_bytes!("../../../icons/tray.svg");

/// Straight (not premultiplied) RGBA for a `px`-square icon, or `None` if the
/// logo fails to parse, which a unit test rules out.
pub(super) fn draw(px: u32, mono: bool) -> Option<Vec<u8>> {
    let tree = Tree::from_data(LOGO_SVG, &Options::default()).ok()?;
    let bounds = tree.root().abs_bounding_box();
    let fit = Fit::new(px, bounds.width(), bounds.height());
    let transform = Transform::from_row(
        fit.scale,
        0.0,
        0.0,
        fit.scale,
        fit.left - bounds.x() * fit.scale,
        fit.top - bounds.y() * fit.scale,
    );
    let mut pixmap = Pixmap::new(px, px)?;
    resvg::render(&tree, transform, &mut pixmap.as_mut());
    Some(
        pixmap
            .pixels()
            .iter()
            .flat_map(|&p| straight(p, mono))
            .collect(),
    )
}

/// `CreateIcon`, behind `tray_icon::Icon::from_rgba`, takes straight alpha;
/// tiny-skia keeps premultiplied.
fn straight(pixel: PremultipliedColorU8, mono: bool) -> [u8; 4] {
    let c = pixel.demultiply();
    if mono {
        [u8::MAX, u8::MAX, u8::MAX, c.alpha()]
    } else {
        [c.red(), c.green(), c.blue(), c.alpha()]
    }
}

/// Where the glyph's bounding box lands in a `px`-square cell.
#[derive(Debug, Clone, Copy, PartialEq)]
struct Fit {
    scale: f32,
    left: f32,
    top: f32,
}

impl Fit {
    /// Tall as the cell allows, one margin (a pixel per 16) clear of the top and
    /// the sides. The bottom gets none: the F ends in a point there, and its
    /// broad top stroke would otherwise make it look like it floats high.
    ///
    /// The top and left edges are whole pixels because both are straight lines
    /// in the logo; a fractional offset would draw each as a two-pixel blur.
    fn new(px: u32, width: f32, height: f32) -> Self {
        let size = px as f32;
        let margin = (px / 16).max(1) as f32;
        let scale = ((size - margin) / height).min((size - 2.0 * margin) / width);
        Self {
            scale,
            left: ((size - width * scale) / 2.0).round(),
            top: margin,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// The logo's glyph box, in its 1024-unit viewBox.
    fn logo() -> (f32, f32) {
        let tree = Tree::from_data(LOGO_SVG, &Options::default()).expect("tray.svg parses");
        let bounds = tree.root().abs_bounding_box();
        (bounds.width(), bounds.height())
    }

    #[test]
    fn every_taskbar_size_draws() {
        // 100 % to 300 %: SM_CXSMICON at each scale Settings offers.
        for px in [16, 20, 24, 28, 32, 36, 40, 48] {
            for mono in [false, true] {
                let rgba = draw(px, mono).expect("glyph draws");
                assert_eq!(rgba.len(), (px * px * 4) as usize);
                assert!(rgba.chunks(4).any(|p| p[3] == u8::MAX), "{px} px is blank");
            }
        }
    }

    #[test]
    fn the_glyph_fills_the_cell_height() {
        let (width, height) = logo();
        let fit = Fit::new(16, width, height);
        assert_eq!(fit.top, 1.0);
        assert!((height * fit.scale - 15.0).abs() < 0.01, "15 of 16 px tall");
        let right = fit.left + width * fit.scale;
        assert!(fit.left >= 1.0 && right <= 15.0, "inside the side margins");
    }

    #[test]
    fn the_glyph_is_centred_on_a_whole_pixel() {
        let (width, height) = logo();
        for px in [16, 20, 24, 32, 48] {
            let fit = Fit::new(px, width, height);
            assert_eq!(fit.left.fract(), 0.0);
            let slack = px as f32 - width * fit.scale;
            assert!((fit.left - slack / 2.0).abs() <= 0.5, "{px} px off centre");
        }
    }

    #[test]
    fn mono_is_white_with_the_colour_alpha() {
        let colour = draw(16, false).expect("colour");
        let mono = draw(16, true).expect("mono");
        for (c, m) in colour.chunks(4).zip(mono.chunks(4)) {
            assert_eq!(c[3], m[3]);
            assert_eq!(&m[..3], &[u8::MAX; 3]);
        }
    }
}
