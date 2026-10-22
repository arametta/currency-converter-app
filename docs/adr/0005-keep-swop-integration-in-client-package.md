# 0005. Keep all swop.cx integration code in the client package

- Status: Accepted
- Date: 2026-10-22

## Context

`SwopProperties` (the swop.* settings) and `SwopClientConfig` (the `RestClient`
setup) were in the `config` package. The architecture rules say that `client` may
only depend on `model`, so `SwopClient` could not read its own settings.

This showed up when retries were tried out: the retry logic needs settings, so it
could not go in `client`, and it ended up as an interceptor in `config`. The rules
passed, but `config` then held swop.cx behavior, not only configuration.

## Decision

Move `SwopProperties` and `SwopClientConfig` into the `client` package. Everything
about swop.cx (settings, HTTP setup, the client, its exceptions, and any future
retries) lives in `client`. The `config` package keeps only app-wide setup: caches
and CORS.

The rule "client depends only on model" stays as it is. The ArchUnit rule for
`RestClient` becomes stricter: only `client` may use it.

## Alternatives considered

- Keep retry-like logic as interceptors in `config`: works, but mixes swop.cx
  behavior into general configuration.
- Allow `client` to depend on `config`: makes the client depend on general
  application setup, and weakens the boundary.

## Consequences

- One place to look for anything about the swop.cx integration.
- The boundary is clearer, and the architecture rules got stricter, not looser.
- `client` now contains Spring configuration classes. That is accepted: they only
  configure the client itself.
