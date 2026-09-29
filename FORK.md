# CamerTrace fork of `whimo-android`

This repository is **CamerTrace** (traçabilité du cacao et du café au Cameroun),
a fork of [EuropeanForestInstitute/whimo-android](https://github.com/EuropeanForestInstitute/whimo-android)
(MIT licence; keep `LICENCE`/`LICENSE` and EFI's copyright notice).

Two goals drive how it is organised:

1. **Keep pulling EFI's work** with as few conflicts as possible.
2. **Contribute back to EFI** with pull requests that contain *no* CamerTrace
   branding, text or configuration.

## Branches

| Branch | Content | Rule |
|---|---|---|
| `main` | exact copy of `upstream/main` | only `git merge --ff-only upstream/main`, never commit here |
| `camertrace` | `main` + brand overlay + Cameroon features | default branch, the one deployed; receives `main` by **merge** |
| `feat/*`, `fix/*` | generic work, candidate for EFI | branch from **`main`**, PR to EFI, then merge into `camertrace` |
| `cm/*` | Cameroon-only work | branch from **`camertrace`**, PR to `prosygma/whimo-android:camertrace` |
| `archive/*` | history before this model (2026-09-29) | read-only |

## First time on a new clone

```bash
scripts/fork-setup.sh     # upstream remote, push to EFI disabled, rerere, merge=ours driver
```

## Pulling EFI's changes

```bash
git fetch upstream
git switch main && git merge --ff-only upstream/main && git push origin main
git switch camertrace && git merge main          # resolve, build, test
git push origin camertrace
```

`rerere` replays conflict resolutions you already made once. Binary brand files
listed in `.gitattributes` with `merge=ours` always keep the CamerTrace version.
Text brand files (theme, config) are **not** auto-resolved on purpose: when EFI
adds a new token or key, you want to see it and give it a CamerTrace value.

## Sending a change to EFI

```bash
git switch -c feat/my-change main     # from main, NOT from camertrace
# ... work, commit (English, generic wording, EFI's defaults) ...
scripts/check-upstream-clean.sh feat/my-change
git push origin feat/my-change        # open the PR on GitHub: base = EFI main
git switch camertrace && git merge feat/my-change
```

Rules for an upstream-bound change:

- never mention CamerTrace, CICC, prosygma, `camertrace.cm`;
- new user-visible text: add the key to EFI's locale files (en, and fr/es when
  you can) with neutral wording; CamerTrace wording goes in the brand overlay;
- new colour, logo or name: add it to the brand layer with EFI's value as the
  default, then give it the CamerTrace value on `camertrace` only.

`check-upstream-clean.sh` fails on the words in `.fork/forbidden-words` and
warns about paths in `.fork/brand-paths`.

## Where the CamerTrace brand lives

The app has a `brand` **product flavor** dimension (from the generic
`feat/white-label` branch, first candidate PR for EFI):

- `whimo`: EFI's look, `com.whimo`;
- `camertrace`: this fork, `whimo.camertrace.cm`.

Build with `./gradlew assembleCamertraceDebug` / `bundleCamertraceRelease`.
Everything CamerTrace-specific lives in **`app/src/camertrace/`**; `app/src/main`
stays as EFI ships it.

| File | Holds |
|---|---|
| `java/.../ui/theme/BrandPalette.kt` | primary, tint, dark surface (Material colour scheme, splash) |
| `res/values*/strings.xml` | name, splash signature, the few strings that name the product |
| `res/values/brand.xml` | line-icon accent, notification icon, splash logo switch |
| `res/values/colors.xml` | XML theme colours |
| `res/drawable/ic_launcher.xml`, `ic_logo_foreground.xml`, `drawable-*/ct_*.png` | adaptive launcher icon, notification icon, splash logo |

`app/google-services.json` (git-ignored) must contain a client for
`whimo.camertrace.cm`. The named colours of `Palette.kt` (ColorSeaBlue…) are
left to EFI on purpose: they also encode traceability statuses.

Brand sources (logos, graphic chart, export scripts) are outside the repo, in
`Documents/whimo/logos/camertrace-cicc/` (`charte-graphique.html`,
`render.sh`, `declinaisons/`).
