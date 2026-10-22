# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this project is

A REST API that converts an amount between currencies using live rates from
swop.cx, plus a small Vue 3 frontend. Java 21, Spring Boot 3.4.1, Maven.
Caffeine for caching, Micrometer metrics exported to InfluxDB, Grafana dashboard,
Docker Compose for the monitoring stack, GitHub Actions CI.

Endpoints: `POST /api/convert` and `GET /api/currencies`. The README documents the
request/response format, the error envelope and the status codes. Treat the README
as the API contract.

## How I work with you

- I make the design decisions. If a task needs a decision that is not written here
  or in `docs/adr/`, stop and ask me. Do not choose on your own.
- Before changing code, tell me the plan in a few lines and wait for my OK.
- Keep changes small. One logical change at a time.
- Do not add new dependencies without asking.
- Do not change the API contract (paths, request/response fields, error envelope,
  status codes) unless I ask for it.

## Commands

Backend (from the repo root):
- Build, run all tests and checks: `./mvnw -B verify` (includes ArchUnit, SpotBugs and the coverage check)
- Run SpotBugs only: `./mvnw -B compile spotbugs:check` (details in `target/spotbugsXml.xml`)
- Coverage report after `verify`: `target/site/jacoco/index.html`
- Run one test class: `./mvnw -B test -Dtest=SwopClientTest`
- Run only the architecture rules: `./mvnw -B test -Dtest=ArchitectureTest`
- Run the app: `SWOP_API_KEY=... ./mvnw spring-boot:run` (fails at startup without the key)
- Full stack with InfluxDB and Grafana: `docker compose up --build`

Frontend (from `frontend/`, needs Node.js 22.12+):
- Install: `npm ci`
- Tests: `npm test`
- Build: `npm run build`
- Dev server: `npm run dev` (backend URL from `VITE_API_BASE`, default `http://localhost:8080`)

After every change, run `./mvnw -B verify` (and `npm test` if you touched the
frontend). A task is not done until both pass.

A Stop hook (`.claude/hooks/verify-on-stop.sh`) runs these checks when you finish,
if backend or frontend files changed. If it reports a failure, fix the code. Do
not change or disable the hook or `.claude/settings.json` without asking me.

## Backend structure

Package root: `com.arametta.currencyconverter`

| Package      | What it holds                                                        |
|--------------|----------------------------------------------------------------------|
| `controller` | REST endpoints and `GlobalExceptionHandler` (maps exceptions to HTTP) |
| `service`    | Conversion logic, currency list, supported-currency check, metrics   |
| `client`     | `SwopClient` (the only HTTP code) and its exceptions                  |
| `model`      | Records: API request/response, error envelope, swop.cx responses      |
| `config`     | Spring configuration: RestClient, caches, CORS, `SwopProperties`      |

### Dependency rules

These rules are enforced by `ArchitectureTest` (ArchUnit), which runs with the
normal tests. If it fails, fix the code. Do not change or weaken the test unless
I ask for it.

- `controller` may use `service` and `model`. It may use the exception types from
  `client` only inside `GlobalExceptionHandler`. Controllers never call `SwopClient`.
- `service` may use `client`, `model` and `config` (for cache names).
- `client` may use `model` only. It knows nothing about services or controllers.
- `model` depends on nothing in this project.
- No package may depend on `controller`.
- Only `SwopClient` talks to swop.cx over HTTP. Do not create a second HTTP client
  or call swop.cx from anywhere else.

## Rules for the code

- **Style.** Follow the formatting of the existing code. Break method chains one
  call per line, and long argument lists one argument per line. Put each validation
  annotation on its own line, like in `ConversionRequest`; short annotations such as
  `@JsonProperty("code") String code` stay on one line. Do not reformat code you
  did not change.
- **SpotBugs.** If SpotBugs reports a finding, fix the code. Do not add exclusions to
  `spotbugs-exclude.xml` without asking me. Every exclusion there has a reason.
- **HTTP client.** Use the `RestClient` bean from `SwopClientConfig`. Never use
  `RestTemplate` or `WebClient`.
- **Configuration.** swop.cx settings live in the `SwopProperties` record
  (`@ConfigurationProperties("swop")`, validated). Add new settings there, not as
  `@Value` fields. Never hardcode the API key or put it in a file.
- **Errors.** Services and the client throw exceptions. Only `GlobalExceptionHandler`
  decides the HTTP status and builds the `ErrorResponse`. Do not return error
  bodies from controllers.
  - Unsupported currency → 422
  - swop.cx unreachable, error, or currency list not loadable → 503
  - Validation failure → 400
- **Currency validation.** `SupportedCurrencies` checks both codes against the cached
  swop.cx list before any rate lookup. Never skip this check, also not when the list
  cannot be loaded (then it is 503).
- **swop.cx free tier.** It only supports EUR as base. Rates are fetched EUR-based
  and converted with a cross rate. Keep it that way.
- **Caching.** Cache names and TTLs are set in `CacheConfig` (rates 5 min,
  currencies 24 h). Do not move them to `application.properties`.
- **Money.** Use `BigDecimal` for amounts and rates, never `double`. The maximum
  amount is 999,999,999,999.99.
- **Metrics.** Metric names are used by the Grafana dashboard. If you rename or
  remove a metric, update `grafana/dashboards/currency-converter.json` in the same
  change, or ask me first.

## Tests

- JUnit 5, Mockito and WireMock. Use WireMock for anything that involves swop.cx,
  so the real `RestClient` setup is tested. Do not use `MockRestServiceServer`.
- New behaviour comes with tests in the same change.
- The build fails if coverage drops below the minimums in `pom.xml` (JaCoCo:
  lines 90%, branches 75%). Never lower them, and never add coverage exclusions,
  to make the build pass. Write the missing tests, or ask me.
- Never delete or weaken a failing test to make the build pass. If you think a test
  is wrong, tell me why and wait.
- Tests must not call the real swop.cx.

## Frontend structure

- `api/` HTTP calls, `composables/` state and logic, `components/` UI,
  `utils/` formatting and validation. `App.vue` only puts the pieces together.
- Components do not call `fetch` directly. They go through `api/currencyApi.js`.
- Amount validation in `utils/validation.js` must match the backend rules.
- Styles stay scoped per component.

## Commits

- One plain imperative line, for example `Validate currencies against the list swop.cx supports`.
- No Conventional Commits prefixes (`feat:`, `fix:`).
- No `Co-Authored-By` lines and no mention of Claude or AI in commits, code or docs.
- Refactors and behaviour changes go in separate commits.
- Do not commit or push unless I ask.

## Do not touch without asking

- `.github/workflows/ci.yml`
- `.claude/settings.json` and `.claude/hooks/`
- `Dockerfile`, `docker-compose.yml`
- `grafana/`
- The README API section

## Design decisions

The reasons behind the main choices are in `docs/adr/` (index in `docs/adr/README.md`).
Read the related ADR before changing caching, the HTTP client, currency validation
or the exchange-rate calculation.

- If a task goes against an accepted ADR, stop and tell me. Do not change the
  decision on your own.
- If a task needs a new design decision, propose a new ADR from
  `docs/adr/template.md` and wait for my OK before writing the code.
- Never edit an accepted ADR to change its decision. A new ADR replaces it.
