# 0003. Use RestClient for calls to swop.cx

- Status: Accepted
- Date: recorded 2026-10-22, the decision is older

## Context

The app first used `RestTemplate`. Spring now recommends `RestClient` for blocking
HTTP calls, and `RestTemplate` only gets maintenance. The timeouts were also
hardcoded, even though the docs said they were configurable.

## Decision

- One `RestClient` bean in `SwopClientConfig`, built from Spring Boot's
  `RestClient.Builder`, with base URL, the API key header and the timeouts.
- All settings in the validated `SwopProperties` record. A missing API key fails at
  startup with a clear message.
- `SwopClient` is the only class that makes HTTP calls. It maps errors to the
  app's own exceptions in one place. This boundary is checked by `ArchitectureTest`.

## Alternatives considered

- Keep `RestTemplate`: works, but it is the older API.
- `WebClient`: made for reactive code. This app is a normal blocking Spring MVC app,
  so it would add the WebFlux dependency for no benefit.

## Consequences

- Modern, fluent API with per-status error handling (`onStatus`).
- Timeouts are now real configuration.
- Tests use WireMock instead of `MockRestServiceServer`, so they exercise the real
  `RestClient` configuration (headers, base URL, timeouts), not a mocked one.
