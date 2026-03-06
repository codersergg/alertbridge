# Versioning Policy

## Format

- Use SemVer with release candidates: `MAJOR.MINOR.PATCH-rc.N`.
- Final release tag has no suffix: `MAJOR.MINOR.PATCH`.
- Docker image tag must match project release version exactly.

## Change Classification

- `MAJOR`: breaking API/contract change.
- `MINOR`: backward-compatible feature.
- `PATCH`: backward-compatible bug fix/refactor/docs-only with no feature behavior expansion.

## RC Rules

- `N` is strictly monotonic for a given `MAJOR.MINOR.PATCH` line.
- Never reuse an already pushed tag name.
- Create RC tag only after successful validation/build for the commit.
- If a published RC is broken, publish the next RC (`rc.N+1`).
- Every successful `jib` release must be explicitly marked by commit subject:
  - `release(jib): <short note>`
  - Example: `release(jib): built and pushed <service>:v1.2.3-rc.4`

## Required Release Order

1. Update project version.
2. Commit version bump.
3. Run validation (tests + build/publish command used by project).
4. Run `jib` publish.
5. Commit release marker: `release(jib): ...` (allowed empty commit).
6. Create git tag for that exact marker commit.
7. Push branch and tag.

## Start-Of-Work Rules

- Before writing code, check whether the latest commit is a published RC (`release(jib): ...` or `HEAD` has tag `vX.Y.Z-rc.N`).
- If latest commit is a published RC, bump to next RC (`rc.N+1`) before code changes.
- If latest commit is not a published RC, do not bump version; continue work under current RC.
- If helper scripts exist, use:
  - `powershell -ExecutionPolicy Bypass -File scripts/release/start-iteration.ps1`

## New Branch Version Start

- On new branch, classify expected scope first:
  - `major` for breaking changes,
  - `minor` for backward-compatible features,
  - `patch` for fixes/refactors/docs.
- Then bump semantic line and reset RC to `rc.1`.
- If helper scripts exist, use:
  - `powershell -ExecutionPolicy Bypass -File scripts/release/start-branch.ps1 -ChangeType minor`

## Multi-Service Compatibility

- For cross-service contract changes, release provider/backend RC first, then consumer/client RC.
- Do not release dependent service RC before dependency contract RC exists.
