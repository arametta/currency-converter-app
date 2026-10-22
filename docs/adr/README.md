# Architecture decision records

Short notes on the main design decisions in this project: what was decided, why,
and what it costs. Code shows what we did; these files show why.

| ADR | Decision | Status |
|-----|----------|--------|
| [0001](0001-eur-based-rates-with-cross-rates.md) | Fetch EUR-based rates and convert with a cross rate | Accepted |
| [0002](0002-cache-rates-per-currency-with-caffeine.md) | Cache rates per currency with Caffeine | Accepted |
| [0003](0003-restclient-for-swop-calls.md) | Use RestClient for calls to swop.cx | Accepted |
| [0004](0004-validate-currencies-against-swop-list.md) | Validate currencies against the swop.cx list before any rate lookup | Accepted |
| [0005](0005-keep-swop-integration-in-client-package.md) | Keep all swop.cx integration code in the client package | Accepted |

## Writing a new ADR

Copy [template.md](template.md), give it the next number, and add it to the table.
Keep it under a page. Do not edit an accepted ADR to change the decision: write a
new one that replaces it, and set the old one's status to "Superseded by 000X".
