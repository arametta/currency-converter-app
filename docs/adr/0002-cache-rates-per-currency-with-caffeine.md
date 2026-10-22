# 0002. Cache rates per currency with Caffeine

- Status: Accepted
- Date: recorded 2026-10-22, the decision is older

## Context

Every conversion needs exchange rates from swop.cx. Calling it for each request is
slow and uses up the API quota. Rates change during the day, but not every second.
The list of currencies almost never changes.

## Decision

Use Spring's cache with Caffeine, configured in `CacheConfig`:

- `exchangeRates`: 5 minutes, max 500 entries. The key is one currency code (for
  example `USD`), not a pair. Because all rates are EUR-based (0001), one cached
  rate is reused for every pair that includes that currency.
- `currencies`: 24 hours, one entry.

The `@Cacheable` method lives in its own bean, `ExchangeRateProvider`. Spring's cache
works through a proxy, and a call from a class to its own method skips the proxy,
so a cached method inside `CurrencyConversionService` would never hit the cache.

## Alternatives considered

- No cache: simple, but slow and expensive in API calls.
- Redis or another shared cache: only needed with several app instances. This app
  runs as one instance, so a local in-memory cache is enough.
- One cache config for all caches (`spring.cache.caffeine.spec`): it applies the
  same TTL to every cache, and these two need very different TTLs.

## Consequences

- Most conversions are answered without calling swop.cx.
- A rate can be up to 5 minutes old. For this use case that is acceptable.
- With several instances, each one would have its own cache. If the app is ever
  scaled out, this decision should be reviewed.
