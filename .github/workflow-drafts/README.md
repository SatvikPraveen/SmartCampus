# Workflow drafts

These workflows replace the ones in `.github/workflows/`, which call Checkstyle, SpotBugs,
PMD, OWASP and Sonar goals that were never configured and therefore always fail.
They live here only because the automation that prepared them was not allowed to write to
`.github/workflows/` (GitHub requires the `workflow` token scope for that).

To activate them:

```bash
git rm .github/workflows/tests.yml
git mv -f .github/workflow-drafts/ci.yml .github/workflow-drafts/release.yml .github/workflow-drafts/codeql.yml .github/workflows/
git rm .github/workflow-drafts/README.md
git commit -m "ci: activate verifiable CI, CodeQL and release workflows"
```

| Workflow | Purpose |
|---|---|
| `ci.yml` | `./mvnw verify` (tests + coverage gate), report upload, benchmark smoke run, Docker build + container smoke test |
| `codeql.yml` | Weekly and per-PR CodeQL security analysis |
| `release.yml` | Builds and attaches the jar to `v*` tag releases |
