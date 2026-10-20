# Security

v0.1.x is the currently maintained release line. Do not put credentials or
customer records in public issues. Use GitHub's private vulnerability reporting
when available, or the contact options in the maintainer's public GitHub profile.

The application binds to `127.0.0.1` by default. All product API handlers require
a bearer token of at least 32 characters; the static UI is public. Tokens live
in browser tab session storage and are never put into a URL. Use a trusted TLS
reverse proxy for remote access and limit API access to trusted operators.

The starter adds its own token check without replacing the host application security chain. Host security rules still apply. Job parameters and errors may contain business data.

The local demo token is a public fixture. Replace it before deployment. The demo
is for synthetic data. There is no multi-user authorization or audit identity in
this release. See [operating limits](docs/usage.md) before exposing a deployment.
