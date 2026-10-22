# 0001. Fetch EUR-based rates and convert with a cross rate

- Status: Accepted
- Date: recorded 2026-10-22, the decision is older

## Context

The swop.cx free tier only returns rates with EUR as the base currency. A request
like USD to GBP cannot be answered with one call for the USD/GBP rate.

## Decision

Always fetch EUR/X rates. For a pair A to B, convert with the cross rate
`rate(EUR/B) / rate(EUR/A)`. EUR itself needs no call: its rate is 1.

## Alternatives considered

- A paid plan with any base currency: costs money for a personal project.
- Another rate provider: swop.cx was already integrated and works well.

## Consequences

- Works on the free tier.
- A conversion between two non-EUR currencies needs two rates. This is mostly free
  because rates are cached per currency (see 0002).
- The result can differ slightly from a direct market rate for the pair. For this
  project that is acceptable.
