# 0004. Validate currencies against the swop.cx list before any rate lookup

- Status: Accepted
- Date: recorded 2026-10-22, the decision is older

## Context

`java.util.Currency` only proves that a code is valid ISO 4217. A valid code that
swop.cx does not support failed later, as a 404 from swop.cx, after an API call
had already been spent.

## Decision

`SupportedCurrencies` checks both codes against the active currency list from
swop.cx before any rate lookup. The list comes from the 24-hour cache (0002), so
this normally costs no extra calls.

- Unsupported code: 422, `Currency not supported: XYZ`, with no rate call.
- The list cannot be loaded: 503. The check is never skipped.
- A 404 from swop.cx on a rate lookup still maps to 422, as a safety net if the
  cached list is out of date.

## Alternatives considered

- Keep only the ISO check: wastes API calls and gives a late, unclear error.
- Store the codes in a `Set` for faster lookup: the list has about 170 entries, so
  scanning it is negligible next to an HTTP call. Kept simple on purpose.
- Skip the check when the list is unavailable: would hide the problem and let bad
  requests through.

## Consequences

- Clear 422 errors, and no wasted rate calls.
- A same-currency request (USD to USD) also needs the list. If swop.cx is down and
  nothing is cached, it returns 503, even though no rate is needed. Accepted to keep
  one validation path for every request.
