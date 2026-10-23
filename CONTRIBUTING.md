# Contributing

Thanks for helping! This is an **unofficial** SDK for the ENZONA API. ENZONA's API reference is only available to registered merchants, so the contract in [docs/api-coverage.md](docs/api-coverage.md) was reconstructed from community SDKs. Do not assume behaviour that is not written there: open an issue with an (anonymized) sandbox response, or add a `TODO: VERIFY WITH ENZONA` entry.

## Ground rules

- **Never** put real credentials, customer data or real payment calls in code, tests, fixtures, issues or PRs. Tests run offline against mocks and fixtures.
- **Never** copy code from reference projects with incompatible licenses (`enzona_api` for Python is GPL-3.0; several others have no license). Read the behaviour, write your own code; see [docs/references.md](docs/references.md).
- Money is `BigDecimal`; do not round. Operations that create, complete, cancel or refund a payment must not be retried automatically.
- Keep the public API small and documented. No Jackson types in public signatures. No new runtime dependency without discussion.
- Java 17 only: compile with `--release 17`; do not use features or APIs from Java 18+.

## Setup

You need JDK 17 (Temurin recommended). Maven is provided by the wrapper.

```bash
./mvnw -B -ntp clean verify      # format check, tests, coverage thresholds, javadoc, jars
./mvnw spotless:apply            # auto-format (google-java-format, AOSP style)
```

Run with JDK 21 as well before opening a PR that touches HTTP or JSON code.

To try the examples against your working copy:

```bash
./mvnw install -DskipTests
./mvnw -f examples/plain-java/pom.xml clean verify
(cd examples/gradle-java && ./gradlew test)
./mvnw -f examples/return-handler/pom.xml clean verify
```

If you change a README code block, edit the example source and run `python3 scripts/readme_snippets.py update`; CI runs `check`.

## Adding or changing an endpoint

1. Record the endpoint in `docs/api-coverage.md` with its source and the verification date (the exported Swagger or a sandbox response beats a community SDK).
2. Add a JSON fixture under `src/test/resources/fixtures/`, anonymized, from a real or documented response.
3. Add the model, the API method (blocking and `...Async`) and contract tests: method, path, headers, body, response, error statuses, and "not retried" when it moves money.
4. Update `CHANGELOG.md` (`[Unreleased]`) and the status column in `docs/api-coverage.md`.

## Commits and pull requests

We use [Conventional Commits](https://www.conventionalcommits.org/), in English:

```
<type>(<scope>): <imperative summary, up to 72 characters>
```

Types: `feat`, `fix`, `test`, `docs`, `build`, `ci`, `refactor`, `chore`, `perf`, `style`.
Scopes: `core`, `auth`, `http`, `json`, `error`, `payments`, `refunds`, `examples`, `docs`, `readme`, `build`, `ci`, `release`, `repo`.

- One logical change per commit, with its tests; every commit must pass `./mvnw clean verify`.
- Breaking changes: `feat(core)!:` plus a `BREAKING CHANGE:` footer.
- Branches are short-lived (`feat/...`, `fix/...`, `docs/...`); `main` stays green and is merged with rebase or fast-forward.
- Releases and publishing are done by the maintainer only (see [docs/release.md](docs/release.md)).

## Reporting bugs

Use the issue templates. Include SDK version, Java version and a minimal sanitized example. For vulnerabilities, follow [SECURITY.md](SECURITY.md) instead of opening a public issue.
