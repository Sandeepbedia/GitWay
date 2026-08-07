#!/usr/bin/env python3
"""
Regenerates the Screenshots section in README.md from whatever images are in
docs/screenshots/.

Usage:
    python3 docs/generate_screenshots.py

How it works:
  - Looks in docs/screenshots/ for files named 1.png, 2.png, 3.png, ... (also
    accepts .jpg/.jpeg/.webp) — any count, there's no upper limit.
  - Sorts them numerically (1, 2, 3, ... 10, 11 — not alphabetically, so 10.png
    doesn't sort before 2.png).
  - Writes a 2-column HTML table into README.md between the
    <!-- SCREENSHOTS:START --> and <!-- SCREENSHOTS:END --> markers, replacing
    whatever was there before.
  - If no numbered images are found, writes a friendly placeholder instead.

Just drop new screenshots into docs/screenshots/ as 1.png, 2.png, 3.png... in
whatever order you want them to appear, then re-run this script. No need to
touch README.md by hand.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCREENSHOTS_DIR = ROOT / "docs" / "screenshots"
README = ROOT / "README.md"

START_MARKER = "<!-- SCREENSHOTS:START -->"
END_MARKER = "<!-- SCREENSHOTS:END -->"

VALID_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp"}


def find_numbered_screenshots() -> list[Path]:
    if not SCREENSHOTS_DIR.exists():
        return []

    numbered = []
    for f in SCREENSHOTS_DIR.iterdir():
        if not f.is_file() or f.suffix.lower() not in VALID_EXTENSIONS:
            continue
        match = re.fullmatch(r"(\d+)", f.stem)
        if match:
            numbered.append((int(match.group(1)), f))

    numbered.sort(key=lambda pair: pair[0])
    return [f for _, f in numbered]


def build_table_markdown(images: list[Path]) -> str:
    if not images:
        return "_No screenshots yet — drop numbered images (1.png, 2.png, ...) into `docs/screenshots/` and re-run `python3 docs/generate_screenshots.py`._"

    rows = []
    for i in range(0, len(images), 2):
        left = images[i]
        left_cell = f'<td width="50%"><img src="docs/screenshots/{left.name}" width="100%" /></td>'
        if i + 1 < len(images):
            right = images[i + 1]
            right_cell = f'<td width="50%"><img src="docs/screenshots/{right.name}" width="100%" /></td>'
        else:
            right_cell = '<td width="50%"></td>'
        rows.append(f"  <tr>\n    {left_cell}\n    {right_cell}\n  </tr>")

    return "<table>\n" + "\n".join(rows) + "\n</table>"


def main() -> int:
    images = find_numbered_screenshots()
    table = build_table_markdown(images)

    text = README.read_text(encoding="utf-8")
    if START_MARKER not in text or END_MARKER not in text:
        print(f"Couldn't find {START_MARKER} / {END_MARKER} markers in README.md — nothing changed.")
        return 1

    before = text.split(START_MARKER)[0]
    after = text.split(END_MARKER)[1]
    new_text = f"{before}{START_MARKER}\n{table}\n{END_MARKER}{after}"

    README.write_text(new_text, encoding="utf-8")
    print(f"README.md updated with {len(images)} screenshot(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
