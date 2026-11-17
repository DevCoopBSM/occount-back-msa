# Build, Test, and Development Commands

- `./gradlew compileKotlin --console=plain`: fast compile check across modules.
- `./gradlew test --console=plain`: runs JUnit 5 test suites.
- `./gradlew clean build --console=plain`: full build, including packaging.

Use module-scoped tasks when changing one area, for example `./gradlew :domains:payment:payment-api:compileKotlin`.

## Local Development (Docker Compose)

- `docker compose up -d`: start all services (MySQL, Kafka, all APIs, gateway).
- `docker compose up -d api-gateway`: start only the gateway.
- `docker compose up -d member-api`: start only the member API.
- `docker compose up -d item-api`: start only the item API.
- `docker compose up -d order-api`: start only the order API.
- `docker compose up -d payment-api`: start only the payment API.
- `docker compose down`: stop all services.
- `docker compose logs -f <service>`: tail logs for a specific service.

## Compile Verification (REQUIRED)

After **every** code change, always run a compile check before considering the task done:

```
./gradlew compileKotlin compileTestKotlin --console=plain
```

For changes scoped to a single domain, prefer the module-scoped variant, e.g.:

```
./gradlew :domains:order:order-application:compileKotlin :domains:order:order-application:compileTestKotlin --console=plain
```

Do not skip this step. Compile errors in related files (imports, constructor changes, deleted classes) are a common source of breakage that must be caught before finishing.

## Pull Requests

For pull requests:

- describe the changed modules and affected flows,
- link the related issue or ticket,
- include test/compile results,
- add request/response examples when changing API behavior.

## Contribution Review Deployment

- Contribution amounts and balances use signed `BIGINT` / Kotlin `Long` (KRW). Clients must preserve 64-bit integers; JavaScript `number` cannot exactly represent every supported value. Reject amounts outside signed 64-bit range; balance overflow fails without committing the request or ledger.
- On an existing member database, stop contribution writes, back up the four contribution tables, and inspect `SHOW CREATE TABLE` before deployment. Run `docker/mysql/migrations/20261110_contribution_review.sql` once against the member database **before** starting the new application. MySQL DDL auto-commits: a failed migration can be partially applied; inspect the schema before resuming, never rerun blindly.
- The migration widens money columns, adds nullable `processed_by` / `processed_at`, and creates indexes for status + requested time, user + requested time, and unfiltered requested time. Do not rely on `ddl-auto=update` as the production migration procedure. Fresh databases receive the same indexes from entity metadata; do not run the existing-table migration after automatic schema creation.
- Existing processed requests retain null audit fields: the original administrator and processing time cannot be reconstructed reliably. New approvals/rejections record both fields. Keep the existing optimistic-lock `version` columns and their data intact.
- Deposit approval promotes only `ROLE_USER` to `ROLE_MEMBER` in the same transaction as the balance, ledger, and request updates. Other roles are unchanged. Existing tokens may retain their old role until refreshed or reissued.
- Contribution approvals serialize on the existing member row before reading the account, including first account creation. Locking is local database work only; no bank/API calls belong inside this transaction. A stale optimistic update returns HTTP 409; re-read the request and account before deciding whether to retry. A non-pending request cannot be approved or rejected again.
- The gateway member route must include `/api/v3/contributions/**`; authentication policy alone does not create a route. The gateway verifies JWT/admin role and overwrites the trusted user-ID header before forwarding to member-api. Member-api must not be exposed directly to untrusted clients.

## Contribution Withdrawal Operations

- Withdrawal approval means internal balance deduction and ledger recording, **not** successful bank transfer. The application has no bank-transfer integration, settlement status, or automatic compensation endpoint. Keep an access-controlled transfer register keyed by withdrawal request ID, including amount, beneficiary, approving administrator, bank reference, and outcome.
- Before sending money, verify the request is APPROVED, its amount matches the withdrawal ledger, and the transfer register has no successful transfer for that request. A timeout is an unknown outcome: reconcile with the bank before retrying to avoid duplicate payment.
- If the bank confirms failure, retain the approved request and immutable withdrawal ledger. Do not re-approve the request, delete the ledger, or manually overwrite the balance. Either retry the confirmed-unpaid transfer using the same request reference, or, if the withdrawal is cancelled, obtain independent administrator approval for a compensating credit.
- For an approved compensating credit, create a new deposit request for exactly the failed withdrawal amount; include the original withdrawal request ID and confirmed bank failure in its memo/evidence. Approve it through the normal audited workflow, record its deposit request ID in the transfer register, and reconcile that the net ledger movement is zero. This is an explicit manual balance restoration, not a bank deposit; prevent duplicate compensation through the transfer register and independent review. Deposit approval follows the same role-promotion policy as any other deposit.
- Never compensate an ambiguous or successful transfer. After either retry or compensation, reconcile the account balance, both request records, ledger before/after balances, and bank statement. Automated settlement tracking and bank-side idempotency are not provided by this API.

## Contribution Integration Verification

- Use JDK 21 for Gradle 8.14.3. Run `./gradlew compileKotlin compileTestKotlin test --continue --console=plain` for repository-wide compilation and regression checks. The Gmail delivery test remains opt-in because it requires real credentials and sends email.
- `ContributionProcessingIntegrationTest` runs five race families 20 times each: first deposits, duplicate approval, concurrent withdrawals, and deposit/withdrawal approval versus rejection. Both transactions establish pending-request snapshots before release; assertions include commit outcomes, balances, ledger continuity, roles, and winning audit fields. Default storage is an isolated H2 database.
- To exercise those tests on a disposable MySQL 8.0 schema, set `CONTRIBUTION_TEST_DATASOURCE_URL`, `CONTRIBUTION_TEST_DATASOURCE_USERNAME`, and `CONTRIBUTION_TEST_DATASOURCE_PASSWORD`, then run `./gradlew :domains:member:member-infrastructure:test --tests '*ContributionProcessingIntegrationTest' --rerun-tasks --console=plain`.
- `ContributionHttpIntegrationTest` starts a real embedded HTTP server with production controller, argument resolver, advice, transaction-proxied use cases, and JPA repositories. Its conflict probe changes a loaded request's version through an independent JDBC connection; the real optimistic failure occurs during Spring transaction commit, and assertions verify HTTP 409 plus rollback of account, approval audit, and ledger writes. The database version bump itself is the independent writer's committed change.
- Run the HTTP test with `./gradlew :domains:member:member-bootstrap:test --tests '*ContributionHttpIntegrationTest' --rerun-tasks --console=plain`. Optional `CONTRIBUTION_HTTP_DB_URL`, `CONTRIBUTION_HTTP_DB_USERNAME`, and `CONTRIBUTION_HTTP_DB_PASSWORD` select MySQL instead of H2.
- For the complete JWT-to-database path, run the real gateway with its `MEMBER_API_URL` pointing at the test member port. Set matching `CONTRIBUTION_HTTP_PORT`, `CONTRIBUTION_GATEWAY_URL`, and plaintext `CONTRIBUTION_GATEWAY_JWT_SECRET` in the HTTP test environment (the gateway's `JWT_SECRET_KEY` is the Base64 encoding of that secret). The optional gateway test verifies invalid/missing JWT 401, non-admin 403, admin access, and replacement of a spoofed identity header. Without gateway settings this test is skipped; the direct-member tests still execute.
- Both external-database test modes use `create-drop`. Use distinct, disposable schemas for the HTTP and race suites; never supply production credentials. The HTTP server intentionally imports the contribution runtime only: mail, Kafka, and external identity verification are outside this verification scope. The migration smoke uses a reconstructed old schema, not a production backup; deployment still requires a backup and inspection of the actual target schema.
