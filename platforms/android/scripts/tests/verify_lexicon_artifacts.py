"""Validate packaged dictionary, original notice and both native ABIs in APK/AAB."""
from pathlib import Path
import struct
import sys
import zipfile
import zlib

WORKSPACE = Path(__file__).resolve().parents[4]
NOTICE = WORKSPACE / "crates/funput-suggestions/data/lexicon/NOTICE.md"


def verify(path):
    prefix = "base/" if path.suffix == ".aab" else ""
    with zipfile.ZipFile(path) as archive:
        lexicon = archive.read(prefix + "assets/lexicon/en.lex")
        magic, version, flags, words, length, heavy, crc = struct.unpack("<4sHHIIII", lexicon[:24])
        assert magic == b"FPLX" and version == 1 and flags == 0
        assert words == 30000 and len(lexicon) <= 524288
        assert len(lexicon) == 24 + ((words + 15) // 16) * 4 + length + heavy * 9
        assert zlib.crc32(lexicon[24:]) == crc
        assert archive.read(prefix + "assets/lexicon/NOTICE.md") == NOTICE.read_bytes()
        for abi in ("arm64-v8a", "x86_64"):
            native = archive.read(prefix + "lib/" + abi + "/libfunput_jni.so")
            assert native[:4] == b"\x7fELF"
            assert b"Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeAttachLexicon" in native
    print(f"{path.name}: {words} words, {len(lexicon)} bytes, CRC {crc:08x}; notice + both JNI ABIs verified")


if __name__ == "__main__":
    for argument in sys.argv[1:]:
        verify(Path(argument))
