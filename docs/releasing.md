# Releasing

## GitHub release

Push a tag that matches the project version:

```sh
git tag v0.3.0
git push origin v0.3.0
```

The [release workflow](../.github/workflows/release.yml) checks that the tag matches
the Maven version, runs the frontend and JVM checks, and publishes the demo JAR, the
core and both starter JARs with their POMs, a source ZIP, `SHA256SUMS` and
`VERIFICATION.json`.

## Maven Central

The `central` Maven profile adds sources, Javadoc and GPG signatures and uploads one
bundle with the parent POM, the core and both starters; the demo is excluded. Publishing is
manual because a Central release cannot be deleted.

One-time setup by the namespace owner:

1. Sign in at [central.sonatype.com](https://central.sonatype.com) with the GitHub
   account `lMysticl`. The `io.github.lmysticl` namespace is verified for that account.
2. Generate a user token (Account → Generate User Token) and add it to this
   repository as the secrets `CENTRAL_TOKEN_USERNAME` and `CENTRAL_TOKEN_PASSWORD`.
3. Create a signing key and publish its public part:

   ```sh
   gpg --quick-gen-key "Pavlo Putrenkov <you@example.com>" ed25519 sign 2y
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>
   ```

   Add the exported private key as `GPG_PRIVATE_KEY` and its passphrase as
   `GPG_PASSPHRASE`.

Then run **Actions → Publish to Maven Central → Run workflow** with the release tag.

To inspect the bundle locally without uploading it (needs any `central` server entry
in `settings.xml` and a local signing key):

```sh
./mvnw -B -Pcentral -DskipTests -Dcentral.dryRun=true deploy
unzip -l target/central-publishing/central-bundle.zip
```
