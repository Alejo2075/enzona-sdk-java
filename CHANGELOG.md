# Changelog

All notable changes to this project are documented here. The format follows [Keep a
Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres to [Semantic
Versioning](https://semver.org/).

## [Unreleased]

Version 2.0.0 is a ground-up rewrite. The 1.x line never worked against the live API (wrong base
path, wrong JSON naming, silently empty responses); see `docs/migration-1.x.md` once published.

### Added
- `EnzonaClient` with production and sandbox environments, OAuth2 client-credentials handling
  (token cached until shortly before it expires, one shared renewal under concurrency) and a single
  repetition after a gateway 401 with a renewed token.
- Payments: create (amount computed and validated like the server does), get, list with filters and
  paging, complete and cancel, each with an `...Async` form.
- Refunds: full and partial refund, get, list with filters and list of one payment's refunds, each
  with an `...Async` form. Creating a refund is never retried.
- Typed exception hierarchy with ENZONA and gateway error codes (`EnzonaErrorCode`).
- Opt-in retry policy for read-only calls only; create, complete and cancel are never retried.
- Lenient JSON reading (numbers as strings, item name as string or array) and strict writing
  (amounts as two-decimal strings, more decimals rejected, never rounded).

### Removed
- The whole 1.x API in `io.github.alejo2075.enzonasdk`. The code stays reachable through the
  `legacy-1.0` tag.
