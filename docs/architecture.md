# Architecture

Design of the ENZONA Java SDK (unofficial, community maintained). It follows the structure of `qvapay-java`, with OAuth2 token handling in place of app-header authentication.

## Baseline

| Item | Decision |
|---|---|
| Minimum runtime | **Java 17 LTS**. Built with JDK 17 and `--release 17`. CI also runs on 21 (and optionally 25). |
| Build | Apache Maven 3.10.0 via the Maven Wrapper (checksum pinned) |
| HTTP | `java.net.http.HttpClient` (JDK), injectable `HttpTransport` for tests |
| JSON | Jackson **3.2.4** (`tools.jackson.core:jackson-databind`), an implementation detail |
| Only runtime dependency | `jackson-databind` (and its transitive dependencies) |
| Tests | JUnit Jupiter 6.1.3, JDK `com.sun.net.httpserver.HttpServer` and an in-memory transport |
| Java package | `io.github.alejo2075.enzona` (groupId `io.github.alejo2075`, artifactId `enzona-sdk`) |

## Layers

```
EnzonaClient ──┬── payments()  PaymentsApi (create, get, list, complete, cancel)
               └── refunds()   RefundsApi  (create, get, list, listForPayment)
               every operation: blocking form + `...Async` form (CompletableFuture)
                         │
                  RequestExecutor (internal) ── builds ApiRequest, attaches the Bearer token,
                         │                      maps ApiResponse → model or exception
                  OAuthTokenProvider ── cached client-credentials token
                         │
                  HttpTransport (interface) ── JdkHttpTransport | test doubles
```

| Package | Contents | Visibility |
|---|---|---|
| `io.github.alejo2075.enzona` | `EnzonaClient`, `Environment`, `PaymentsApi`, `RefundsApi`, `EnzonaVersion`. API classes live here so their constructors stay package-private. | public |
| `.auth` | `ClientCredentials`, `Scope`, `AccessTokenProvider`, `OAuthTokenProvider` | public |
| `.http` | `HttpTransport`, `JdkHttpTransport`, `ApiRequest`, `ApiResponse`, `RetryPolicy` | public (extension points) |
| `.model` | immutable request and response types | public |
| `.flow` | `ReturnUrlParser` | public |
| `.exception` | exception hierarchy, `EnzonaErrorCode` | public |
| `.internal` | `JsonCodec`, `RequestExecutor`, `ErrorMapper`, `Money`, `UrlEncoding`, `Validation` | not part of the API |

## Key decisions

### Models
- Responses are Java 17 `record`s with camelCase names; the wire format is snake_case through one naming strategy in the mapper.
- Requests are final classes with validating builders.
- **Money** is a `BigDecimal` written as a string with exactly two decimals (`"10.00"`), because the API demands it. A value with more than two decimals is rejected at `build()`; the SDK never rounds silently. When reading, a JSON number or a string is accepted.
- `CreatePaymentRequest.build()` computes `amount.total` from items, shipping, tax, tip and discount and checks it against server rule `4084`; item tax must add up to `details.tax` (rule `4083`).
- Statuses are an enum (`PaymentStatus`) with `UNKNOWN` plus the raw numeric code, so new codes never break parsing.
- Unknown JSON properties are ignored.

### Tolerant deserialization
The API is inconsistent about types (see [api-coverage.md](api-coverage.md#response-fields-payment-refund)). Dedicated deserializers in `internal.JsonCodec` accept number-or-string for integers and decimals and string-or-array for `items[].name` and `status_denom`. Every variant has a fixture. Serialization is strict: only the documented forms are written.

### Jackson encapsulation
No Jackson type appears in a public signature. Only `com.fasterxml.jackson.annotation` annotations may decorate model records. The mapper is built once, immutable and thread-safe. A test keeps Jackson 2 on the classpath to prove coexistence.

### Authentication
`OAuthTokenProvider` requests `POST {base}/token` with `Authorization: Basic base64(key:secret)` and `grant_type=client_credentials&scope=…`.

- The token is cached until `expires_in` minus a safety margin (60 s by default), measured with an injectable `Clock`.
- Concurrent callers share one in-flight token request.
- If an API call answers **401**, the token is invalidated, a fresh one is requested and the call is repeated **once**. This also applies to money operations: the gateway rejects an invalid token before the request reaches the backend, so nothing can have been executed. The behaviour is documented and tested.
- ENZONA has no refresh token, so there is no refresh flow.
- A custom `AccessTokenProvider` can replace the default one (for example to share a token between processes).
- `ClientCredentials.toString()` and every exception redact secrets and tokens.
- The scope separator is a configurable detail (`Scope`) pending confirmation.

### Errors
All exceptions are unchecked and extend `EnzonaException`.

| Situation | Exception |
|---|---|
| 400 | `ValidationException` |
| 401 | `AuthenticationException` |
| 403 | `PermissionDeniedException` |
| 404 | `NotFoundException` |
| 429 | `RateLimitException` (carries `Retry-After` when present) |
| 5xx | `ServerException` |
| other non-2xx | `EnzonaApiException` |
| token request failed | `TokenException` |
| I/O, timeout, interruption | `TransportException` |
| JSON problems | `SerializationException` |

`EnzonaApiException` exposes `statusCode()`, `errorCode()` (the numeric code from `{"code":…}` or `{"fault":{"code":…}}`), the message and description. It never contains request headers or the request body. Code must branch on `errorCode()` or `EnzonaErrorCode`, not on message text, because the messages are in Spanish and may change.

### Retries
Disabled by default. `RetryPolicy` is opt-in and applies only to `GET` operations. `create`, `complete`, `cancel` and `refund` are never retried (the single 401 repetition above is the one exception). Backoff is exponential with jitter and honours `Retry-After`. `merchant_op_id` and `commerce_refund_id` are not treated as idempotency keys.

### Sync and async
Every operation is defined once and exposed twice: a blocking method (`create`) and a `CompletableFuture` method with an `Async` suffix. The blocking path preserves thread interruption. Async failures complete the future exceptionally with the typed exception as the cause.

### HTTP behaviour
- Production `https://api.enzona.net`, sandbox `https://apisandbox.enzona.net`, chosen with `Environment`. A custom base URL must be HTTPS unless `allowInsecureBaseUrlForTesting()` is called.
- Redirects are never followed (`Redirect.NEVER`), so the Bearer token cannot be forwarded to another host.
- Headers: `User-Agent: enzona-sdk-java/<version>`, `Accept: application/json`, `Content-Type: application/json` for bodies.
- Path segments and query values are percent-encoded. A `transaction_uuid` containing `/` cannot change the route.
- Connect and request timeouts are configurable. `EnzonaClient` is `AutoCloseable`.
- The `href` values inside `links` are never called with the token. `confirm` is for the buyer's browser; `complete` and `cancel` are done through the SDK.

### Payment flow and the lack of webhooks
PaymentAPI has no signed callbacks. The buyer returns to `return_url`, which proves nothing by itself. The integrator must fetch the payment, compare amount, currency and `merchant_op_id` with the local order, and then call `complete`. `flow.ReturnUrlParser` only extracts the `transaction_uuid` from the return URL. See `docs/payment-flow.md`.

### Logging
The core has no logging dependency and logs nothing.

## Extensibility
The QR API (`/qr/v1.0.0`, scope `enzona_business_qr`) uses the same token provider, executor and error mapping. Adding it means a new `QrApi` class, its models, fixtures and a scope constant, with no change to the HTTP layer. A QR notification verifier will be added only once a signature scheme is documented.
