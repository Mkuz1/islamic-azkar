# Islamic Azkar: project status

_Source of truth for this app's claude.ai chat. As of 2026-11-27._

Hisn al-Muslim in Arabic, English and French, Liquid Glass design, prayer times and home-screen widgets. صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم.

## Current state

| | |
|---|---|
| Version | **1.0.25** |
| Live branch | **`main-d0fcuk`** is where the current app and website live; `main` is far behind. This status file is kept on `main` (the default branch, which claude.ai reads) |
| Platforms | Android, iPhone/iPad (+ widgets), Mac, **Apple TV**, Windows, web reader |
| App Store | **Apple TV only is live**: id6835170109. iPhone/iPad and Mac were resubmitted 2026-11-21 and are not live yet |
| Google Play | Not live; Android is APK only |
| Downloads | Public repo **Mkuz1/islamic-azkar-releases** (`v1.0.25`, 2026-11-19) |
| Website | **https://islamicazkar.net** (EN/AR/FR landing + **web reader** at `/app/`), deploys from `main-d0fcuk` via `website.yml` to Pages project `islamic-azkar` |

## Rules specific to this app

- Landing pages are **generated** by `tools/build_landing.py` (EN/AR/FR, on `main-d0fcuk`). Edit its `T` dict / template, never only `website/*.html`. The koussa.net invite (`more` key) lives there.
- On `main-d0fcuk`, pushes that touch `RELEASE` or `store/*/SUBMIT|UPLOAD|SIGN` fire releases and App Store / Mac / TV submissions. Don't touch those files unless a release is intended.

## Recent changes

- 2026-11-26: Website reader available again from the landing page; Apple TV live badge (PR #4); koussa.net invite (PR #3); SEO per language (PR #6); app's dark green / gold redesign; dedication on the landing page; iPhone Duo App Store screenshots; local QA notes added (PR #1, device IDs removed).
- 2026-11-21: Mac App Store 1.0.25 submitted; support page islamicazkar.net/support; all three Apple platforms resubmitted.

## Open items

- **PR #2** (deploy from main) and **PR #5** (main-d0fcuk → main) left open on purpose: #2 would deploy main's old site over the live one; #5 would fire App Store/Mac/TV submissions and a release. Recommended: close both.
- iPhone/iPad and Mac App Store approval; then update the site and `MK-APPS/build.py`.

## Rules for every MK app (decided in the MK APPS project)

- **This file is the hand-off to the app's own Claude chat.** That chat reads it from GitHub through Project knowledge. Every change to this app (code, release, store, website, decision) must also update this file in the same PR or commit.
- **Nothing local-only.** Every asset, store file and site file lives in the app's GitHub repo. Signing keys and passwords are the only exception and are never committed (they stay in `~/keys` on the Mac).
- **Websites deploy themselves** to Cloudflare Pages from GitHub Actions on push (secrets `CLOUDFLARE_API_TOKEN` / `CLOUDFLARE_ACCOUNT_ID` are set on the repo). No manual or terminal deploys.
- **Every app site invites visitors to https://www.koussa.net** (the MK Apps showcase, source in `Mkuz1/MK-APPS`, served by Cloudflare project `mkuzapps`). Keep that card when editing the site, and update the app's row in `MK-APPS/build.py` when platforms, versions or store links change.
- **SEO and share previews (done 2026-11-26):** keep the OG + Twitter tags with an absolute 1200x630 image under 300 KB, canonical, favicon.ico + 192/512 icons + 180 px touch icon, web manifest, robots.txt, sitemap.xml and SoftwareApplication JSON-LD. When replacing a share image, give it a new file name so WhatsApp refreshes its cache.
- **Store badges only for what is really live.** Re-check the App Store / Play page before adding a badge.
- **Mohamad merges pull requests himself** (Claude Code's auto mode on the Mac blocks `gh pr merge`).

---
_Last updated 2026-11-27. Update this file with every change to the app._
