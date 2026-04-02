# Contributing

## Commit Conventions

**Format:** `<type>(<scope>): <subject>`

| Type | When |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code change, no behavior change |
| `test` | Test additions or changes |
| `docs` | Documentation only |
| `chore` | Build, tooling, deps, scripts |
| `perf` | Performance improvement |

**Rules:**
- Subject: imperative mood, lowercase, no period (`add user auth` not `Added user auth.`)
- Scope: optional, use module name from AGENTS.md (e.g. `evcs`, `ess`, `ui`, `edge`, `backend`)
- Body: explain *why*, not *what* (wrap at 72 chars)
- Breaking changes: add `!` after type/scope + `BREAKING CHANGE:` footer

**Examples:**
```
feat(evcs): add constraint-based dynamic pricing core
fix(edge): handle null channel value in modbus bridge
chore: update deploy-edge task for new Pi target
refactor(ess): extract balancing strategy into separate class

feat!: remove legacy v1 JSON-RPC endpoints

BREAKING CHANGE: /api/v1/* routes removed, migrate to /api/v2/*
```

## Testing Conventions

**Run tests (Java):** `mise run test`

**Run tests (UI):** `mise run test:ui`

| Rule | Detail |
|---|---|
| Coverage reporting | Codecov (CI uploads JaCoCo + Karma reports) |
| Java test location | `<bundle>/test/` alongside `src/` |
| Java test naming | `*Test.java` (JUnit 4) |
| UI test location | `ui/src/**/*.spec.ts` |
| UI test naming | `*.spec.ts` (Karma/Jasmine) |
| Test types | Unit (both), integration (Java bundles) |

**Rules:**
- MUST write tests for new public Java methods and Angular pipes/utils.
- MUST NOT commit with failing tests (CI enforces this via Gradle + ng test).
- Use JUnit 4 for Java -- do not introduce JUnit 5 without team discussion.
- Use Karma/Jasmine for UI -- do not mix with Jest.
- Keep tests focused: one logical concern per test case.

## Code Style

- **Java:** Checkstyle enforced -- run `mise run lint` before committing.
- **UI:** ESLint -- run `mise run lint:ui` before committing.
- MUST pass both linters before opening a PR (CI blocks on failures).
- OSGi `@Component` annotations required for all Edge/Backend services -- no plain Spring or non-OSGi DI.
- New OSGi bundles must be added to `EdgeApp.bndrun` or `BackendApp.bndrun` and resolved (`mise run resolve`).

## Pull Request Process

1. Branch from `main`.
2. Keep PRs focused: one logical change per PR.
3. All CI checks MUST pass before merge (Checkstyle, Java build + test, UI build + lint + test).
4. Require at least one reviewer approval.
5. For bndrun changes: run `mise run resolve` locally and commit the result.
6. Deploy task changes (`.mise/tasks/`) must be tested against a real or simulated Pi target before merging.
