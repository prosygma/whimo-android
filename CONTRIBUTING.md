# Contributing to CamerTrace (Android app)

CamerTrace is the cocoa and coffee traceability platform of the CICC (Conseil Interprofessionnel
du Cacao et du Café, Cameroon). It is a fork of [WHIMO](https://github.com/EuropeanForestInstitute/whimo-android)
by the European Forest Institute (EFI), under the MIT licence.

Thank you for helping. This page explains where to send a change and what we check before
merging. [`FORK.md`](FORK.md) explains the branch model in more detail.

## Branches

| Branch | What it holds |
|---|---|
| `main` | An exact copy of EFI's `main`. Nobody commits here. |
| `camertrace` | `main` plus the CamerTrace brand and the Cameroon features. This is the default branch and what is deployed. |
| `feat/*`, `fix/*` | Generic work that every WHIMO deployment could use. Starts from `main`. |
| `cm/*` | Cameroon-only work. Starts from `camertrace`. |
| `archive/*` | Old history, read-only. |

## Where to send your change

1. **Is it useful to every WHIMO deployment?** (a bug fix, a generic feature)
   Branch from `main` as `feat/<topic>` or `fix/<topic>`.
   - Prefer opening the pull request **at EFI**: `EuropeanForestInstitute/whimo-android`, base `main`.
     CamerTrace gets it at the next EFI sync.
   - If CamerTrace needs it sooner, open a second pull request from the same branch against
     `camertrace` here, and mention the EFI one.
2. **Is it only for CamerTrace or Cameroon?** (CICC rules, wording, brand, local integrations)
   Branch from `camertrace` as `cm/<topic>` and open the pull request against `camertrace` here.

Work on your own fork and open pull requests from it. Keep each pull request to one topic.

### First time on a clone

```bash
git remote add upstream https://github.com/EuropeanForestInstitute/whimo-android.git
scripts/fork-setup.sh   # disables push to EFI, enables rerere and the merge=ours driver
```

## Rules for every change

- **Brand values live in the brand layer only.** For Android that is the `camertrace` product flavor (`app/src/camertrace/`); the `whimo` flavor is EFI's version.
  Components use semantic names (`primary`, `surface-dark`, …), never CamerTrace colours or names.
- **Status colours are not brand colours.** Traceability and transaction status colours stay
  EFI's values.
- **Text.** New text for a generic feature goes into EFI's locale files, with neutral wording.
  CamerTrace wording goes into the brand layer. Give at least English and French.
- **No secrets and no real personal data**, in code, tests, fixtures, screenshots, or issues.
  A CICC card identifies a farmer: use made-up values shaped like real ones. See [`SECURITY.md`](SECURITY.md).
- **Generic branches must stay clean.** Before opening a `feat/*` or `fix/*` pull request, run:

  ```bash
  git fetch upstream
  scripts/check-upstream-clean.sh <your-branch>
  ```

  It fails if the branch adds a CamerTrace-specific word, and warns if it touches brand files.
  CI runs it too.
- **Commits.** Write a short imperative summary (about 72 characters at most), and say *why* in
  the body. Don't mix formatting changes with real changes.

## Develop and test

Requirements: JDK 17 and the Android SDK (platform 36).

```bash
cp config/build.properties.example config/build.properties      # API URL and app settings
cp app/google-services.json.example app/google-services.json     # placeholder Firebase config
# optional: config/secrets.properties from config/secrets.properties.example (Mapbox token, Google client ID)

./gradlew --no-daemon testCamertraceDebugUnitTest assembleCamertraceDebug
```

The placeholder files are enough to build and run unit tests; maps, push notifications and Google
sign-in need real values. Never commit `build.properties`, `secrets.properties`,
`signing.properties`, `google-services.json` or a keystore (they are gitignored).

- Never wrap the `Application`'s base context: the framework casts it when it delivers broadcasts,
  and the app crashes (see `WhimoApp`).
- `SeaBlue`, `MidnightBlue` and `MulberryPurple` also encode traceability states: don't recolour them.
- After changing a native dependency, check that the arm64 libraries stay 16 KB-aligned
  (`readelf -lW <lib>.so | grep LOAD`), or Google Play refuses the release.
- Releases are built and signed by maintainers only; don't bump `versionCode` in a pull request.

CI runs the same checks on every pull request. A pull request needs a passing CI and an approval
from a maintainer (`CODEOWNERS`) before it is merged.

## Licence

By contributing, you agree that your contribution is released under the [MIT licence](LICENSE)
of this repository.
