#!/usr/bin/env python3
"""
Regenerates the Screenshots section in README.md from images inside
docs/screenshots/.

Usage:
    python3 docs/generate_screenshots.py

Features:
  - Supports 1.png, 2.png, 3.png ... unlimited screenshots.
  - Supports .png, .jpg, .jpeg, .webp.
  - Sorts screenshots numerically.
  - Generates a responsive 2-column HTML table.
  - Automatically updates README.md between markers.
  - No manual README editing required.
"""

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent

SCREENSHOTS_DIR = ROOT / "docs" / "screenshots"
README = ROOT / "README.md"

START_MARKER = "<!-- SCREENSHOTS:START -->"
END_MARKER = "<!-- SCREENSHOTS:END -->"

VALID_EXTENSIONS = {
    ".png",
    ".jpg",
    ".jpeg",
    ".webp",
}


def find_numbered_screenshots():
    """
    Find screenshots like:
    1.png
    2.png
    3.webp
    """

    if not SCREENSHOTS_DIR.exists():
        return []

    images = []

    for file in SCREENSHOTS_DIR.iterdir():

        if not file.is_file():
            continue

        if file.suffix.lower() not in VALID_EXTENSIONS:
            continue

        match = re.fullmatch(r"(\d+)", file.stem)

        if match:
            number = int(match.group(1))
            images.append((number, file))

    images.sort(key=lambda x: x[0])

    return [file for _, file in images]


def build_screenshot_table(images):
    """
    Creates a 2 column HTML table.
    """

    if not images:
        return (
            "_No screenshots yet — add images like "
            "`1.png`, `2.png`, `3.png` inside "
            "`docs/screenshots/` and run the generator._"
        )

    rows = []

    for index in range(0, len(images), 2):

        left = images[index]

        left_html = (
            f'    <td width="50%">\n'
            f'      <img src="docs/screenshots/{left.name}" '
            f'width="100%" />\n'
            f'    </td>'
        )

        if index + 1 < len(images):

            right = images[index + 1]

            right_html = (
                f'    <td width="50%">\n'
                f'      <img src="docs/screenshots/{right.name}" '
                f'width="100%" />\n'
                f'    </td>'
            )

        else:

            right_html = (
                '    <td width="50%"></td>'
            )


        rows.append(
            "  <tr>\n"
            f"{left_html}\n"
            f"{right_html}\n"
            "  </tr>"
        )


    return (
        "<table>\n"
        +
        "\n".join(rows)
        +
        "\n</table>"
    )


def update_readme():

    if not README.exists():
        print("README.md not found.")
        return 1


    images = find_numbered_screenshots()

    table = build_screenshot_table(images)


    content = README.read_text(
        encoding="utf-8"
    )


    pattern = (
        re.escape(START_MARKER)
        +
        r".*?"
        +
        re.escape(END_MARKER)
    )


    replacement = (
        START_MARKER
        +
        "\n"
        +
        table
        +
        "\n"
        +
        END_MARKER
    )


    updated = re.sub(
        pattern,
        replacement,
        content,
        flags=re.DOTALL
    )


    if updated == content:

        print(
            "Screenshot markers not found. "
            "README unchanged."
        )

        return 1


    README.write_text(
        updated,
        encoding="utf-8"
    )


    print(
        f"README updated with {len(images)} screenshot(s)."
    )

    return 0



if __name__ == "__main__":
    sys.exit(update_readme())
