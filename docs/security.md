# Security

What the SDK does for you, and what stays your responsibility. To report a vulnerability see [SECURITY.md](../SECURITY.md).

## Secrets

| Secret | Where it goes | Where it never goes |
|---|---|---|
| Consumer key and secret | environment variables or a secret manager, on your server | source code, a Git repository, a browser, a mobile app, logs |
| Access token | held in memory by the SDK for about an hour | disk, logs, URLs |

- The consumer secret is sent **only** to the token endpoint, as HTTP Basic authentication. API calls carry the Bearer token.
- `ClientCredentials.toString()`, `EnzonaClient.toString()`, the token provider, requests and every exception hide the secret and the token. Tests check messages, causes and stack traces of every failure path, and that the SDK prints nothing to the console.
- Sandbox and production use different keys. Use `Environment.SANDBOX` while developing.
- If a secret leaks, regenerate it in the ENZONA API Store and redeploy.

## Transport

- HTTPS is required. A plain `http` base URL is refused unless you call `allowInsecureBaseUrlForTesting()`, which is meant for loopback test servers.
- Redirects are never followed, so a hostile or misconfigured response cannot make the SDK send your Bearer token to another host.
- Identifiers and query values are percent-encoded: a `transactionUuid` such as `../x` cannot change the path of a request.
- The `href` of a payment's `links` is never called with your token. The `confirm` link is for the buyer's browser.

## Money

- The redirect to your `returnUrl` proves nothing. Verify the payment (status, amount, currency, operation id) before you complete it and before you deliver anything. See [payment-flow.md](payment-flow.md).
- Handle returns idempotently; they can arrive twice.
- Operations that create, complete, cancel or refund a payment are **never retried** by the SDK. A repeated `complete` or `refund` could charge or pay twice. After a transport error, look the payment up before trying again.
- There is one deliberate repetition: if the gateway answers 401 because the access token is no longer valid, the SDK gets a new token and repeats the call once. That is safe because the gateway refuses an invalid token before the request reaches ENZONA, so nothing was executed. A 401 that carries an ENZONA business code is not repeated.
- Amounts are `BigDecimal` and are never rounded. A value with more than two decimals is rejected.

## Error handling

- Branch on `errorCode()` (a number) or `knownErrorCode()`, never on the message text.
- Do not show ENZONA error messages to your buyers; they can be internal. The [return handler example](../examples/return-handler) answers with a fixed text.

## Dependencies

The only runtime dependencies are Jackson 3 (`tools.jackson`) and `jackson-annotations`. Jackson never appears in the public API. Dependabot proposes updates, and CI scans for secrets and reviews dependency changes in pull requests.

## Not covered

The SDK cannot protect a server that is itself compromised, validate who your buyer is, or detect a fraudulent but valid payment. ENZONA's own controls apply to the payer side.
