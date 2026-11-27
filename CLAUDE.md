# Islamic Azkar: notes for Claude

## Keep PROJECT_STATUS.md current

`PROJECT_STATUS.md` at the repo root is what this app's claude.ai chat reads (via Project knowledge, GitHub). With every change to this app (code, release, version bump, store submission, website, or a decision), update `PROJECT_STATUS.md` in the same commit or PR: version table, recent changes (dated), and open items. Keep it factual and short.

The live app and website are on branch `main-d0fcuk`, but PROJECT_STATUS.md is kept on `main` (the default branch the claude.ai chat reads). When working on `main-d0fcuk`, also update PROJECT_STATUS.md on `main` with a small separate PR.
