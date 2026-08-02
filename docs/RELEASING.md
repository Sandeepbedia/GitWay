# Releasing a new version

Publishing a new version is a single `git tag` push — `.github/workflows/release.yml`
does the rest: it builds the release APK and creates a GitHub Release with the APK
attached, the same as filling out
[`/releases/new`](https://github.com/Sandeepbedia/GitWay/releases/new) by hand.

## Steps

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Commit that change.
3. Tag it and push the tag:

   ```bash
   git tag v1.0
   git push origin v1.0
   ```

   Tags must start with `v` (`v1.0`, `v1.1`, `v2.0.3`, ...) — that's what the
   workflow's trigger (`tags: ["v*"]`) matches on. **The first release should be
   tagged `v1.0`.**

4. Watch the **Actions** tab — the `Release APK` workflow builds the APK and
   publishes it as a release automatically. No manual steps on github.com needed.

## Signed vs. unsigned APKs

By default (no secrets configured) the workflow still runs and publishes an
**unsigned** release APK — `app/build.gradle.kts` is written to build fine without a
keystore present. To get a properly **signed** APK instead, add these four
repository secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | Your `.jks` file, base64-encoded: `base64 -w0 your-release-key.jks` |
| `KEYSTORE_PASSWORD` | The keystore's store password |
| `KEY_ALIAS` | The key alias inside the keystore |
| `KEY_PASSWORD` | The key's password |

Once those exist, every future tagged release is signed automatically — nothing
else changes about the process above.

## In-app update check

The app itself checks `GET /repos/Sandeepbedia/GitWay/releases/latest` on launch
(see `MainActivity.kt` / `GitHubRepository.checkForUpdate`) and shows an **Update
available** dialog if the latest tag is newer than the installed `versionName`. This
means step 3 above (`git push origin vX.Y`) is also what makes existing installs
start seeing the update prompt — there's nothing extra to configure.
