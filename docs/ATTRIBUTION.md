# Third-party attribution

Git Way is licensed under the [GPL-3.0](../LICENSE). The code in this repository is
the project's own work; this file records the origin and license of every piece of
**third-party material that ships inside the APK**, as GPL-3.0 §5(d) requires.

## Bundled artwork

### File-type icons — `app/src/main/res/raw/*.svg` (186 files)

These are the small monochrome/bicolour glyphs shown for a file's extension in the
Repository Browser and the Analysis screen.

| Icon(s) | Source | License |
|---|---|---|
| `github.svg` | [Devicons](https://devicons.github.io/) ("Original Developer Community Icon") | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| Remaining 185 | Original artwork created for Git Way | GPL-3.0 (same as the project) |

> When you add a new icon, record its source and license in the table above **in the
> same pull request**. If you copied or traced it from somewhere, it needs a credit
> line here; a bare file is not enough.

### Banner and screenshots

| File | Source | License |
|---|---|---|
| `docs/banner.png` | Created for Git Way | GPL-3.0 |
| `docs/screenshots/*.png` | Captured from the app | GPL-3.0 |

### App launcher icon

| File | Source | License |
|---|---|---|
| `app/src/main/res/mipmap-*/ic_launcher*.png` | Created for Git Way | GPL-3.0 |

## Trademarks

Some bundled icons depict third-party product logos (language and platform marks).
Those logos remain the property of their respective owners. They are used here
**only to identify the technology a file belongs to** — this is nominative use, not
a claim of endorsement or affiliation. No logo has been modified beyond scaling and
recolouring for dark/light themes.

If you are the owner of a mark shown here and would like it removed or replaced, open
an issue or pull request and it will be taken down.

## Runtime dependencies

No third-party code is vendored into the repository. AndroidX, Kotlin, Compose,
Retrofit, OkHttp, kotlinx.serialization, Coil and friends are resolved as normal
Gradle dependencies from Google Maven / Maven Central and carry their own licenses
(Apache-2.0, MIT and similar). Their license texts travel with the artifacts.
