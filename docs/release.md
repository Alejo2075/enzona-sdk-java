# Release guide

How to publish the SDK to Maven Central. **Nothing here runs automatically.** Publishing needs the maintainer's explicit decision and several things only the maintainer can provide (see [Human setup required](#human-setup-required)).

## Release history

| Version | Date | Notes |
|---|---|---|
| 0.9.5, 1.0 | 2024 | The old implementation. It never worked against the live API (see [migration-1.x.md](migration-1.x.md)). Published versions are immutable, so they stay on Central. |
| 2.0.0 | 2026-10-23 | The rewrite. Same `groupId` and `artifactId`. Signed with key `BC2FB2385127DC8345884595564BA523FA3BCCF6` (expires 2028-10-22, renew before then). Verified from clean Maven and Gradle consumers that resolve it only from Maven Central, and with `gpg --verify` of every file. Released **before** being validated against the ENZONA sandbox: the contract comes from community SDKs. |

## Baseline

| Item | Value |
|---|---|
| Coordinates | `io.github.alejo2075:enzona-sdk` (same namespace and Central Portal account as `qvapay-java`) |
| First version of the rewrite | `2.0.0` (development uses `2.0.0-SNAPSHOT`, local only; snapshots are never published) |
| Runtime / bytecode | Java 17 LTS (`--release 17`), verified on 17 and 21 |
| Build | Maven Wrapper 3.10.0 with a pinned checksum (do not use Maven 4 for publishing) |
| Publisher | `org.sonatype.central:central-publishing-maven-plugin:0.12.0` (Central Portal), `autoPublish=false` |
| Signing | `maven-gpg-plugin:3.2.8`, only in profile `central` |
| Artifacts | `.jar`, `-sources.jar`, `-javadoc.jar`, `.pom`, `.asc` signatures, checksums |

The old OSSRH / Nexus staging flow (`s01.oss.sonatype.org`) used by the 1.x POM is not used. A published version is immutable: a mistake is fixed with a **new** version, never by re-uploading.

## Human setup required

These steps cannot be done by the build or by an assistant. Tick them as you do them.

- [x] Public GitHub repository `Alejo2075/enzona-sdk-java`.
- [x] Namespace `io.github.alejo2075` verified in the [Central Portal](https://central.sonatype.com/publishing/namespaces) (it is the namespace of the already published `qvapay-java`).
- [x] Confirm the license and maintainer data in the POM (`Apache-2.0`, developer *Alejandro Santana*, as in the published 1.x).
- [x] A **Portal user token** (Central Portal → Account → *Generate User Token*). The one used for `qvapay-java` works.
- [x] A GPG key whose public half is on a keyserver (`BC2FB2385127DC8345884595564BA523FA3BCCF6`, created for `qvapay-java`, expires 2028-10-22). Keep the private key out of the repository.
- [x] GitHub environment `maven-central` in **this** repository, with a required reviewer, limited to protected branches.
- [x] Environment secrets `CENTRAL_TOKEN_USERNAME`, `CENTRAL_TOKEN_PASSWORD`, `GPG_PRIVATE_KEY` (ASCII-armored export) and `GPG_PASSPHRASE`. Load them with `gh secret set NAME --env maven-central --repo Alejo2075/enzona-sdk-java`; for the key: `gpg --armor --export-secret-keys BC2FB2385127DC8345884595564BA523FA3BCCF6 | gh secret set GPG_PRIVATE_KEY --env maven-central --repo Alejo2075/enzona-sdk-java`.
- [x] `main` protected: pull request, required status checks, no force-push, linear history. Private vulnerability reporting, secret scanning with push protection and Dependabot alerts enabled.
- [ ] **Export the Swagger of the Payment API from the ENZONA API Store** into `docs/spec/` and compare it with [api-coverage.md](api-coverage.md). Until this is done the contract rests on community SDKs.
- [ ] **A manual run against the sandbox** (see below) and the real responses saved, anonymized, as fixtures.
- [x] Explicit approval to publish (given for 2.0.0; each release needs its own).

## One-time machine setup (for local releases)

```bash
gpg --list-secret-keys --keyid-format long        # the key above should be listed
```

`~/.m2/settings.xml` (never commit it):

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>PORTAL_TOKEN_USERNAME</username>
      <password>PORTAL_TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

## Procedure

1. **Decide the version** (SemVer) and update `CHANGELOG.md`: move the `[Unreleased]` entries under `## [X.Y.Z] - YYYY-MM-DD`.
2. **Set the release version**, no `-SNAPSHOT`: `./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false`. Update the version in `examples/*` and in the README installation snippets.
3. **Check the POM**: license, developer, SCM URL. `./mvnw dependency:tree` should show only Jackson.
4. **Verify everything** with JDK 17 and again with JDK 21:
   ```bash
   ./mvnw -B -ntp clean verify
   ./mvnw -B -ntp install -DskipTests
   ./mvnw -B -ntp -f examples/plain-java/pom.xml clean verify
   ./mvnw -B -ntp -f examples/return-handler/pom.xml clean verify
   (cd examples/gradle-java && ./gradlew test)
   (cd examples/gradle-groovy && ./gradlew test)
   python3 scripts/readme_snippets.py check
   ```
5. **Build the signed bundle without uploading anything** and inspect it. Do it in a **disposable copy** of the repository:
   ```bash
   ./mvnw -B -ntp -Pcentral -DskipUpload=true -DskipTests clean deploy
   python3 scripts/verify_bundle.py target/central-publishing/central-bundle.zip
   ```
   > **Careful:** `deploy` also runs the `install` phase, so this puts `X.Y.Z` into your `~/.m2`. A release version that is not on Central yet would then shadow the real one for local builds. After the dry run delete `~/.m2/repository/io/github/alejo2075/enzona-sdk/X.Y.Z` and remove the version from `maven-metadata-local.xml`.

   The check needs only the GPG key, no token. The verifier checks the five files per artifact, the `.asc` signatures (it runs `gpg --verify`), the checksums, the required POM metadata and that the bytecode is Java 17 (class file major 61). With a `-SNAPSHOT` version the plugin builds no bundle.
6. **Commit and tag**: `chore(release): prepare X.Y.Z`, then an annotated tag `git tag -a vX.Y.Z -m "vX.Y.Z"` on that commit. Pushing the tag does **not** publish anything.
7. **Upload for validation** with the protected workflow: GitHub → Actions → *Release (manual)* → *Run workflow* on `main` with the version. A reviewer must approve the `maven-central` environment. The workflow checks version, tag and changelog, re-runs the full verification and the example consumers, signs, and uploads to the Portal. Locally it would be `./mvnw -Pcentral -DskipTests clean deploy`.
8. **Review and publish manually** at <https://central.sonatype.com/publishing/deployments>. Because `autoPublish=false`, an upload is never a publication. Wait for the state *Published*.
9. **Verify resolution** from clean consumers once the artifact is visible (it can take a while to sync): a temporary Maven project with the dependency and no extra repositories, and a Gradle project with only `mavenCentral()`. Remove `mavenLocal()` from the Gradle examples and run them against Central.
10. **Update the repository** only after step 9: real coordinates and the Maven Central badge in `README.md` and `README.es.md` (replace the "not yet published" notes), a GitHub Release from the tag, and bump to the next `-SNAPSHOT`.
11. **Rotate credentials** if anything leaked. Never reuse a version number. Rollback means publishing a corrected new version and documenting it in the changelog.

## Manual check against the sandbox (never in public CI)

With sandbox credentials in `ENZONA_CONSUMER_KEY` / `ENZONA_CONSUMER_SECRET` (and from a network that can reach `apisandbox.enzona.net`, which usually means Cuba):

1. Create a payment with `QuickStart` and open the confirmation URL.
2. Confirm it in the ENZONA sandbox app, `get` it, `complete` it, `get` it again.
3. Create another payment and `cancel` it.
4. Refund part of the completed payment, list its refunds.
5. Record every difference with [api-coverage.md](api-coverage.md) (especially the open questions at its end) and save the responses, anonymized, in `src/test/resources/fixtures/`.

Do not run this against production with real money without a conscious decision.

## Optional: Javadoc site

`qvapay-java` publishes its Javadoc to GitHub Pages with a workflow. This repository does not have one yet; add it only after enabling Pages, and link it from the README.

## Never

- Publish without the maintainer's approval, or from a fork.
- Put tokens, GPG keys or passphrases in Git, logs or workflow files.
- Claim the package is on Maven Central before a verified link exists.
