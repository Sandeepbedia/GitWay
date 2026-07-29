# Contributing to Git Way

First off — thanks for considering a contribution. Bug fixes, small features, docs
fixes, and UI polish are all genuinely welcome. This guide covers everything you
need to get a change from your machine into the app.

> **Before you start:** this project uses a [source-available, no-redistribution
> license](LICENSE). You're free to fork, build, and send Pull Requests — you're
> just not able to publish your own copy of the compiled app anywhere. See the
> LICENSE for the full terms.

## Table of contents

- [Ways to contribute](#ways-to-contribute)
- [Development setup](#development-setup)
- [Project conventions](#project-conventions)
- [Making a change](#making-a-change)
- [Pull request checklist](#pull-request-checklist)
- [Reporting bugs](#reporting-bugs)
- [Code of conduct](#code-of-conduct)

## Ways to contribute

- **Bug fixes** — the most valuable kind of PR. Include repro steps in the description.
- **Small features** — open an issue first to discuss scope before investing time in a
  large PR; saves everyone a re-write.
- **UI/UX polish** — screenshots or a short screen recording in the PR description
  help a lot here.
- **Documentation** — README, this file, or inline KDoc comments.
- **Bug reports & feature requests** — even without code, a clear issue is a real
  contribution.

## Development setup

**Requirements**

- Android Studio (latest stable)
- JDK 17+
- An Android device or emulator running **API 26 (Android 8.0)** or higher

**Steps**

```bash
git clone https://github.com/<your-fork>/Git_Way.git
cd Git_Way
cp keystore.properties.example keystore.properties   # debug builds work without editing this
```

Open the folder in Android Studio and let Gradle sync. Run the `app` configuration
on a device/emulator — no additional setup, API keys, or backend is required, since
Git Way talks directly to the GitHub REST API using a Personal Access Token you
paste in at runtime (nothing is hardcoded).

> `keystore.properties` is git-ignored. Release (signed) builds need your own
> keystore; debug builds work out of the box without one.

## Project conventions

- **Language:** Kotlin only, 100% Jetpack Compose UI (no XML layouts).
- **Architecture:** MVVM — `ui/` (Composables + ViewModels) → `domain/` (pure
  business logic, no Android framework imports where avoidable) → `data/` (network,
  local storage, repository implementations). See [README.md](README.md#project-structure)
  for the full folder map.
- **Naming:** standard Kotlin style — `PascalCase` for classes/composables,
  `camelCase` for functions/properties. Composable functions are `PascalCase` since
  they're effectively UI "constructors".
- **Comments:** favor a short KDoc explaining *why* a non-obvious decision was made
  over restating *what* the code does line-by-line.
- **No new dependencies** without a good reason — open an issue first if a change
  needs one; this keeps the app small and the attack surface (Smart Upload
  Protection, secret scanning, etc.) easy to reason about.
- **Formatting:** match the existing style in the file you're editing (trailing
  commas, 4-space indent, `import` ordering). Android Studio's default Kotlin
  formatter is fine.

## Making a change

1. **Fork** the repository and create a branch off `main`:
   ```
   git checkout -b fix/short-description
   ```
   Branch prefixes: `fix/`, `feat/`, `docs/`, `refactor/`, `chore/`.
2. Make your change. Keep the diff focused — one logical change per PR is much
   easier to review than five unrelated ones bundled together.
3. **Test on-device.** This app touches real file I/O (Storage Access Framework)
   and the real GitHub API — please actually run the flow you changed (folder
   scan → compare → upload) rather than relying on it "looking right" in code.
4. Commit with a clear message:
   ```
   git commit -m "Fix: blob upload retry didn't back off on 503"
   ```
5. Push and open a Pull Request against `main`.

## Pull request checklist

- [ ] The app builds and runs (`./gradlew assembleDebug`)
- [ ] You tested the actual flow your change touches, on a real device or emulator
- [ ] No secrets, tokens, or personal keystore files are included in the diff
- [ ] The PR description explains **what** changed and **why** (link an issue if
      one exists)
- [ ] Screenshots/recording attached for any UI change

## Reporting bugs

Open an issue with:

- What you expected to happen vs. what actually happened
- Steps to reproduce
- Android version + device (or emulator) you saw it on
- A screenshot if it's a UI issue — this app relies on GitHub's Git Data API for
  pushes, so for upload failures please also paste the exact error text shown
  (Git Way surfaces GitHub's real error message, not just an HTTP status code)

## Code of conduct

Be respectful, assume good faith, and keep feedback focused on the code. Anything
else (harassment, personal attacks, etc.) will get a comment removed or a
contributor blocked from the repository — this is a small personal project, not a
place for drama.

---

Questions before diving in? Open an issue and ask — happy to point you in the
right direction before you write a big PR.
