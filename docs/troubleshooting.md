# Troubleshooting

Match what you see with the cause. Numeric codes come from `EnzonaApiException.errorCode()`.

## I cannot reach ENZONA at all

`TransportException`, timeouts, or `TokenException: The token request could not be completed`.

- The ENZONA hosts (`api.enzona.net`, `apisandbox.enzona.net`, `bulevar.enzona.net`) are **frequently unreachable from outside Cuba**. Run from a server that can reach them.
- Raise the timeouts if the network is slow: `requestTimeout(Duration.ofSeconds(60))`.
- Check proxies and firewalls: the SDK uses the JDK `HttpClient`, which honours the JVM's proxy settings (`-Dhttps.proxyHost`).

## `TokenException`

| Message | Meaning |
|---|---|
| `The token endpoint rejected the request (HTTP 401)…` | wrong consumer key or secret, or you mixed sandbox keys with the production environment (or the reverse) |
| `…(HTTP 4xx/5xx)…` | the application may be disabled or not subscribed to the Payment API in the API Store |
| `…unreadable response` / `has no access_token` | something between you and ENZONA returned a page instead of JSON (a proxy, a captive portal) |

The message never includes the response body or your secret.

## `AuthenticationException` (HTTP 401)

- Code **900901 / 900902**: the gateway does not accept the token. The SDK already tried once with a new token. Check that the application is subscribed to the Payment API and that the scope is `enzona_business_payment`.
- Any other code: ENZONA rejected the operation for your account.

## `PermissionDeniedException` (HTTP 403)

The application's scopes or subscriptions do not allow the operation. Request the scope you need with `EnzonaClient.builder().scopes(Scope.PAYMENT, Scope.QR)`.

## `ValidationException` (HTTP 400)

| Code | Meaning | What to do |
|---|---|---|
| 4082, 4092 | the payment structure is wrong | compare your request with [api-coverage.md](api-coverage.md) |
| 4083 | the item taxes do not add up to the tax in the breakdown | use the builder: it computes them |
| 4084 | the breakdown does not add up to the total | use the builder; do not set `total` by hand when you have items |
| 4065 | you tried to refund (or complete) a payment that is not confirmed | wait for the buyer, check the status with `get` |
| 4067 | the payment was already refunded in full | nothing to do |
| 4068, 4069 | the refund exceeds what can be refunded | look at `refundedAmount()` and the payment's refunds |
| 4070 | the refund amount is zero | pass an amount greater than zero |
| 4016 | the currency is not configured | check the currency code (`CUP`) |

## `NotFoundException` (HTTP 404)

Code 4064 / 4066: the payment or refund does not exist **for this merchant**. Check the identifier and that you are on the same environment where you created it.

## `RateLimitException` (HTTP 429)

The gateway throttled you (codes 900800 and similar). Wait `retryAfter()` if present. For reads you can enable retries: `retryPolicy(RetryPolicy.exponentialBackoff(3, Duration.ofMillis(200), Duration.ofSeconds(2)))`.

## `IllegalArgumentException` when building a request

The builder rejects the data before it reaches ENZONA: an amount with more than two decimals (the SDK never rounds), a missing `returnUrl`, a total that does not match the items, a quantity below 1. The message names the field.

## A `Payment` field is `null`

Every response field is optional because ENZONA does not always send it. Use the convenience methods (`status()`, `statusDenom()`, `confirmationUrl()`) and check `null`. Dates are plain strings because the format is not documented.

## The response is empty / looks wrong

Run the same call in the sandbox and compare with the fixtures in `src/test/resources/fixtures`. The fixtures were reconstructed from community SDKs, not captured from ENZONA. If you find a difference, please open an issue with the (anonymized) response so the model and the coverage notes can be fixed.

## Still stuck

Open an issue with the SDK version, the environment (sandbox or production), the exception class and `errorCode()`. **Never paste your consumer secret or an access token.**
