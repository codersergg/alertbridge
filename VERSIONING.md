# Versioning Policy

## Format

- Use SemVer with release candidates: `MAJOR.MINOR.PATCH-rc.N`.
- Final release tag has no suffix: `MAJOR.MINOR.PATCH`.
- Production tags in `-prod.N` format are deprecated and must not be used for new releases.
- Docker image tag must match project release version exactly.

## Branching Policy Reference

- Unified branch workflow rules are defined in:
  - `D:\Projects\LingFlow\lingflow-docs\system\DOC_GOVERNANCE.md`

## Mandatory Branch Workflow Rules

- Feature work is allowed only in a dedicated working branch; direct feature development in `main/master` is forbidden.
- Creating or deleting any working/feature/fix branch is allowed only after explicit agreement.
- If there is uncertainty about the target RC line/working branch, stop implementation and clarify first.
- Fixes must use separate `fix/*` branches from `main/master`.
- Before merge into `main/master`, add a marker commit `finalize(feature): ...`; after merge the branch is considered closed for new feature work.
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
- Creating or pushing stable tag before marker commit is forbidden.

## RC Rules

- `N` is strictly monotonic for a given `MAJOR.MINOR.PATCH` line.
- Never reuse an already pushed tag name.
- Create RC tag only after successful validation/build for the commit.
- If a published RC is broken, publish the next RC (`rc.N+1`).
- Every RC `jib` release must be explicitly marked by commit subject in the working branch (`feature/*` or `release/*`):
  - `release(jib): <short note>`
  - Example: `release(jib): built and pushed <service>:v1.2.3-rc.4`
- RC git tag must reference the marker release commit; immediate next commit is allowed only if it explicitly references marker commit hash in commit message.
- Creating or pushing RC tag before marker release commit is forbidden.

## Required Release Order

1. Update project version.
2. Commit version bump.
3. Run validation (tests + build/publish command used by project).
4. Run image publish for the target registry (for example `jib` or project-specific publish command).
5. Verify pushed image exists in registry for the exact target tag.
6. Create a new release file in `lingflow-deploy/infra/releases/` from the latest `release-*.env`, then update only image tags/variables for services deployed in this release window (including environment-specific tags like `*_TEST_IMAGE`).
7. Commit RC release marker in the working branch: `release(jib): ...` (empty commit allowed).
8. Create RC git tag for that exact marker commit.
9. Push branch and tag.
## Multi-Service Compatibility

- For cross-service contract changes, release provider/backend RC first, then consumer/client RC.
- Do not release dependent service RC before dependency contract RC exists.




