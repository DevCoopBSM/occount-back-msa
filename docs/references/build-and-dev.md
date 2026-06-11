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
