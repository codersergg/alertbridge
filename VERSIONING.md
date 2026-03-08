# Versioning Policy

## Format

- Use SemVer with release candidates: `MAJOR.MINOR.PATCH-rc.N`.
- Final release tag has no suffix: `MAJOR.MINOR.PATCH`.
- Production tags in `-prod.N` format are deprecated and must not be used for new releases.
- Docker image tag must match project release version exactly.

## Branching Policy Reference

- Branch naming and merge rules are defined in:
  - `D:\Projects\LingFlow\lingflow-docs\BRANCHING_POLICY.md`

## Change Classification

- `MAJOR`: breaking API/contract change.
- `MINOR`: backward-compatible feature.
- `PATCH`: backward-compatible bug fix/refactor/docs-only with no feature behavior expansion.

## PATCH Bump Rule (3rd Digit)

- Bump `PATCH` (`X.Y.Z -> X.Y.(Z+1)`) only for backward-compatible fixes without new feature scope:
  - production bug fix,
  - dependency/security update without contract change,
  - refactor/internal cleanup with unchanged behavior.
- For a new patch release line, start from `X.Y.(Z+1)-rc.1`.
- If work is feature scope, use a new `MINOR` line; if breaking, use a new `MAJOR` line.

## Stable Release Rules

- Stable tags (`vX.Y.Z`) are allowed only from `main` branch.
- Stable image build/publish must complete first (`jib` to target registry).
- After successful stable publish, create a mandatory marker commit with subject `release(jib): ...`.
- Create stable git tag only on that marker commit, then push commit and tag.

## RC Rules

- `N` is strictly monotonic for a given `MAJOR.MINOR.PATCH` line.
- Never reuse an already pushed tag name.
- Create RC tag only after successful validation/build for the commit.
- If a published RC is broken, publish the next RC (`rc.N+1`).
- Every RC `jib` release must be explicitly marked by commit subject in the working branch (`feature/*` or `release/*`):
  - `release(jib): <short note>`
  - Example: `release(jib): built and pushed <service>:v1.2.3-rc.4`

## Required Release Order

1. Update project version.
2. Commit version bump.
3. Run validation (tests + build/publish command used by project).
4. Run `jib` publish.
5. Verify pushed image exists in ECR for the exact target tag.
6. Only after ECR verification, update `lingflow-deploy` release file (`infra/releases/release-*.env`) with the new tag.
7. Commit RC release marker in working branch: `release(jib): ...` (allowed empty commit).
8. Create RC git tag for that exact marker commit.
9. Push working branch and RC tag.

## Start-Of-Work Rules

- Before writing code, check whether the latest commit is a published RC (`release(jib): ...` or `HEAD` has tag `vX.Y.Z-rc.N`).
- If latest commit is a published RC, bump to next RC (`rc.N+1`) before code changes.
- If latest commit is not a published RC, do not bump version; continue work under current RC.
- If helper scripts exist, use:
  - `powershell -ExecutionPolicy Bypass -File scripts/release/start-iteration.ps1`

## New Branch Version Start

- On new branch, open a new semantic release line:
  - `minor` by default for planned feature work,
  - `major` only for planned breaking changes.
- Initial version on the new branch must start from `MAJOR.MINOR.PATCH-rc.1`.
- Do not continue previous line RCs on a newly created branch.
- If helper scripts exist, use:
  - `powershell -ExecutionPolicy Bypass -File scripts/release/start-branch.ps1 -ChangeType minor`
  - `powershell -ExecutionPolicy Bypass -File scripts/release/start-branch.ps1 -ChangeType major`

## Multi-Service Compatibility

- For cross-service contract changes, release provider/backend RC first, then consumer/client RC.
- Do not release dependent service RC before dependency contract RC exists.