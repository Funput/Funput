"""Exercise the Phosphor SVG -> vector drawable conversion; no network."""
from importlib.util import module_from_spec, spec_from_file_location
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

SCRIPT = Path(__file__).resolve().parents[1] / "import-phosphor-icons.py"
_spec = spec_from_file_location("import_phosphor_icons", SCRIPT)
importer = module_from_spec(_spec)
_spec.loader.exec_module(importer)

ANDROID = "{http://schemas.android.com/apk/res/android}"


def svg(*children, view_box="0 0 256 256"):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view_box}" fill="currentColor">'
            + "".join(children) + "</svg>")


class ConversionTest(unittest.TestCase):
    def test_every_path_becomes_a_tintable_path(self):
        vector = importer.svg_to_vector(svg('<path d="M0,0H8"/>', '<path d="M8,8H16"/>'), "t")
        root = ET.fromstring(vector)
        self.assertEqual("256", root.get(ANDROID + "viewportWidth"))
        paths = root.findall("path")
        self.assertEqual(["M0,0H8", "M8,8H16"], [p.get(ANDROID + "pathData") for p in paths])
        self.assertTrue(all(p.get(ANDROID + "fillColor") == "#FF000000" for p in paths))

    def test_shapes_other_than_paths_are_refused_not_dropped(self):
        with self.assertRaises(importer.IconError):
            importer.svg_to_vector(svg('<path d="M0,0H8"/>', '<circle r="4"/>'), "t")

    def test_a_different_grid_is_refused(self):
        with self.assertRaises(importer.IconError):
            importer.svg_to_vector(svg('<path d="M0,0H8"/>', view_box="0 0 24 24"), "t")

    def test_conversion_is_deterministic(self):
        source = svg('<path d="M1,1H2"/>')
        self.assertEqual(importer.svg_to_vector(source, "t"), importer.svg_to_vector(source, "t"))


class ManifestTest(unittest.TestCase):
    def test_reads_names_weights_and_comments(self):
        text = "# header\nkeyboard\ngear-six fill  # tab, selected\n\ncheck bold\n"
        self.assertEqual(
            [("keyboard", "regular"), ("gear-six", "fill"), ("check", "bold")],
            importer.read_manifest(text),
        )

    def test_rejects_an_unknown_weight(self):
        with self.assertRaises(importer.IconError):
            importer.read_manifest("keyboard duotone\n")

    def test_resource_names_are_android_safe(self):
        self.assertEqual("ph_gear_six_fill", importer.resource_name("gear-six", "fill"))
        self.assertEqual("ph_caret_right", importer.resource_name("caret-right", "regular"))


if __name__ == "__main__":
    unittest.main()
