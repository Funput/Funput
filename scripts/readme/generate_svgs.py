"""Generate the README visuals: an animated typing demo and a three-pillar card.

Usage: python3 scripts/readme/generate_svgs.py assets/readme
"""
import sys
from pathlib import Path

OUT = Path(sys.argv[1])
OUT.mkdir(parents=True, exist_ok=True)

FONT = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"
MONO = "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace"

THEMES = {
    "light": dict(bg="#FFFFFF", border="#E6E3DA", ink="#1F2023", muted="#7A7D73",
                  key="#F4F2EC", keyline="#DCD8CC", keyink="#3A3C36", chip="#F4F2EC"),
    "dark": dict(bg="#15171C", border="#2A2D35", ink="#F3F3F0", muted="#9A9DA6",
                 key="#22252D", keyline="#353946", keyink="#D9DAE0", chip="#22252D"),
}

GRADIENT = """<linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
  <stop offset="0" stop-color="#FF7A3D"/><stop offset="0.5" stop-color="#F0388C"/><stop offset="1" stop-color="#8B5CF6"/>
</linearGradient>"""

# (method, [(key, text_after_key)])
SCENES = [
    ("Telex", [("T", "T"), ("i", "Ti"), ("e", "Tie"), ("e", "Tiê"), ("s", "Tiế"), ("n", "Tiến"),
               ("g", "Tiếng"), ("␣", "Tiếng "), ("V", "Tiếng V"), ("i", "Tiếng Vi"), ("e", "Tiếng Vie"),
               ("e", "Tiếng Viê"), ("t", "Tiếng Viêt"), ("j", "Tiếng Việt")]),
    ("VNI", [("X", "X"), ("i", "Xi"), ("n", "Xin"), ("␣", "Xin "), ("c", "Xin c"), ("h", "Xin ch"),
             ("a", "Xin cha"), ("2", "Xin chà"), ("o", "Xin chào")]),
]
STEP, HOLD, GAP = 0.32, 1.8, 0.25
W, H = 880, 280


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def typing_demo(t):
    frames = []  # (start, end, method, keys, index, text)
    now = 0.0
    for method, steps in SCENES:
        keys = [k for k, _ in steps]
        frames.append((now, now + GAP, method, keys, -1, ""))
        now += GAP
        for i, (_, text) in enumerate(steps):
            dur = HOLD if i == len(steps) - 1 else STEP
            frames.append((now, now + dur, method, keys, i, text))
            now += dur
    total = now
    css, groups = [], []
    for n, (s, e, method, keys, idx, text) in enumerate(frames):
        a, b = s / total * 100, e / total * 100
        if n == 0:
            kf = f"0%,{b:.3f}%{{opacity:1}}{b + 0.001:.3f}%,100%{{opacity:0}}"
        elif n == len(frames) - 1:
            kf = f"0%,{a - 0.001:.3f}%{{opacity:0}}{a:.3f}%,100%{{opacity:1}}"
        else:
            kf = (f"0%,{a - 0.001:.3f}%{{opacity:0}}{a:.3f}%,{b:.3f}%{{opacity:1}}"
                  f"{b + 0.001:.3f}%,100%{{opacity:0}}")
        css.append(f"@keyframes f{n}{{{kf}}}.f{n}{{opacity:0;animation:f{n} {total:.2f}s infinite}}")
        caps, kw, gapk = [], 44, 8
        row = len(keys) * kw + (len(keys) - 1) * gapk
        x0 = (W - row) / 2
        for k, key in enumerate(keys):
            x = x0 + k * (kw + gapk)
            if k == idx:
                fill, stroke, ink = "url(#g)", "none", "#FFFFFF"
            elif k < idx:
                fill, stroke, ink = t["key"], t["keyline"], t["keyink"]
            else:
                fill, stroke, ink = "none", t["keyline"], t["muted"]
            lift = -3 if k == idx else 0
            caps.append(
                f'<rect x="{x:.1f}" y="{196 + lift}" width="{kw}" height="44" rx="10" fill="{fill}" stroke="{stroke}"/>'
                f'<text x="{x + kw / 2:.1f}" y="{224 + lift}" text-anchor="middle" font-family="{MONO}" '
                f'font-size="18" font-weight="600" fill="{ink}">{esc(key)}</text>')
        groups.append(
            f'<g class="f{n}">'
            f'<text x="{W / 2}" y="40" text-anchor="middle" font-family="{FONT}" font-size="13" font-weight="700" '
            f'letter-spacing="2" fill="{t["muted"]}">{method.upper()}</text>'
            f'<text x="{W / 2}" y="140" text-anchor="middle" font-family="{FONT}" font-size="68" font-weight="700" '
            f'fill="{t["ink"]}">{esc(text)}<tspan class="caret" fill="url(#g)">|</tspan></text>'
            + "".join(caps) + "</g>")
    css.append("@keyframes blink{0%,49%{fill-opacity:1}50%,100%{fill-opacity:0}}"
               ".caret{font-weight:300;animation:blink 1s step-end infinite}")
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" role="img" '
            f'aria-label="Typing Tiếng Việt in Telex and Xin chào in VNI">'
            f"<defs>{GRADIENT}</defs><style>{''.join(css)}</style>"
            f'<rect x="1" y="1" width="{W - 2}" height="{H - 2}" rx="24" fill="{t["bg"]}" stroke="{t["border"]}"/>'
            + "".join(groups) + "</svg>")


ICONS = {
    # 24x24 stroke icons, drawn inside a gradient disc
    "heart": '<path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.2a4.3 4.3 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z"/>',
    "lock": '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
    "devices": '<rect x="2" y="4" width="14" height="10" rx="1.5"/><path d="M6 18h6"/>'
               '<rect x="17" y="8" width="5" height="12" rx="1.2"/>',
}
PILLARS = {
    "vi": [("heart", "Miễn phí", "Không quảng cáo, không tài khoản"),
           ("lock", "Riêng tư", "Chữ bạn gõ ở lại trên máy"),
           ("devices", "Năm nền tảng", "Gõ máy nào cũng như nhau")],
    "en": [("heart", "Free", "No ads, no account"),
           ("lock", "Private", "What you type stays on device"),
           ("devices", "Five platforms", "Same typing on every device")],
}


def pillars(t, lang):
    pw, ph = 880, 170
    col = pw / 3
    parts = []
    for i, (icon, title, sub) in enumerate(PILLARS[lang]):
        cx = col * i + col / 2
        parts.append(
            f'<circle cx="{cx}" cy="52" r="30" fill="url(#g)"/>'
            f'<g transform="translate({cx - 14} 38) scale(1.1667)" fill="none" stroke="#FFFFFF" stroke-width="1.9" '
            f'stroke-linecap="round" stroke-linejoin="round">{ICONS[icon]}</g>'
            f'<text x="{cx}" y="120" text-anchor="middle" font-family="{FONT}" font-size="21" font-weight="700" '
            f'fill="{t["ink"]}">{esc(title)}</text>'
            f'<text x="{cx}" y="146" text-anchor="middle" font-family="{FONT}" font-size="15" '
            f'fill="{t["muted"]}">{esc(sub)}</text>')
        if i:
            parts.append(f'<line x1="{col * i}" y1="34" x2="{col * i}" y2="140" stroke="{t["border"]}"/>')
    label = " | ".join(f"{a}: {b}" for _, a, b in PILLARS[lang])
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {pw} {ph}" width="{pw}" height="{ph}" '
            f'role="img" aria-label="{esc(label)}"><defs>{GRADIENT}</defs>' + "".join(parts) + "</svg>")


for name, theme in THEMES.items():
    (OUT / f"typing-demo-{name}.svg").write_text(typing_demo(theme), encoding="utf-8")
    for lang in PILLARS:
        (OUT / f"pillars-{lang}-{name}.svg").write_text(pillars(theme, lang), encoding="utf-8")
print(sorted(p.name for p in OUT.iterdir()))
