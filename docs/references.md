# Reference study

ENZONA does not publish openly indexable API documentation. The official reference is the WSO2 API Store at `https://api.enzona.net/store/` (sandbox `https://apisandbox.enzona.net/store/`). You can only browse it with a merchant account registered at <https://bulevar.enzona.net>, and on 2026-10-23 those hosts timed out from outside Cuba. The contract used by this SDK was therefore rebuilt from the community projects below. Each of them was read in full: README, source and license.

Study date: **2026-10-23**. All repositories are inactive, so their latest commit is listed.

| Repository | Latest commit | Kind | License |
|---|---|---|---|
| [daxslab/enzona-payment-python](https://github.com/daxslab/enzona-payment-python) | `9a77214` (2020-05-16) | swagger-codegen client of PaymentAPI v1.0.0 | none in the repository: read only |
| [daxslab/enzona-payment-php](https://github.com/daxslab/enzona-payment-php) | `a3dd17d` (2019-10-17) | swagger-codegen client of PaymentAPI v1.0.0 | MIT (`composer.json`) |
| [daxslab/enzona-qr-python](https://github.com/daxslab/enzona-qr-python) | `94562cb` (2020-05-15) | swagger-codegen client of QR API v1.0.0 | none: read only |
| [luis901101/enzona](https://github.com/luis901101/enzona) (pub.dev `enzona`) | `74d51e6` (2021-10-14) | hand-written Dart/Flutter SDK | BSD-3-Clause style |
| [NoxCreation/enzona_api](https://github.com/NoxCreation/enzona_api) (PyPI `enzona-api`) | `a973683` (2022-09-04) | hand-written Python wrapper used in production | **GPL-3.0** |
| [uicdevs/wp-enzona-api-integration](https://github.com/uicdevs/wp-enzona-api-integration) | `460bb96` (2020-05-05) | WordPress plugin | none: read only |

No code from these projects is copied into this SDK. They are used as evidence of the wire contract only.

## Findings per source

### daxslab payment clients (generated from the official Swagger)

- Base path `https://apisandbox.enzona.net/payment/v1.0.0`; OAuth2 scope `enzona_business_payment`.
- 14 routes (see [api-coverage.md](api-coverage.md)), all request bodies in snake_case.
- Request model for creating a payment (`Payload2`): `description, currency, amount{total, details{shipping, tax, discount, tip}}, items[]{name, description, quantity, price, tax}, merchant_op_id, invoice_number, return_url, cancel_url, terminal_id, buyer_identity_code`. **`merchant_uuid` is not part of it.**
- List routes accept `merchant_uuid, limit, offset, merchant_op_filter, status_filter, start_date_filter, end_date_filter, order_filter`, plus `transaction_uuid` and `commerce_refund_id` for refunds.
- Every response is typed as a bare `object`. The response models in the spec (`PaymentFullOperations`, `PaymentRefundOperations`, `Pagination`…) are informative only and disagree with the real responses on types.
- Payer-side operations (`/confirm`, `/payments/store`, `/payments/vendor/code`) take the buyer's `payment_password` and `funding_source_uuid`.

### daxslab QR client

- Base path `/qr/v1.0.0`, scope `enzona_business_qr`.
- `POST /qr/merchant` with `amount, currency, description, return_url, notify_url` returns `vendor_identity_code, create_at, update_at, image`.
- `GET /qr/{qr_code}` returns `qr_code, amount, currency, description, name, avatar, require_password, username`.

### luis901101/enzona (Dart): the best evidence about real responses

- Token from `{apiUrl}/token` with client credentials. The comments say ENZONA has **no refresh token**, so the SDK requests a new token.
- Routes match the daxslab spec (`/payment/v1.0.0/...`).
- Lists come back as `{"payments": [...], "pagination": {...}}` and `{"refunds": [...], "pagination": {...}}`. `pagination` has `first, prev, next, last, total, total_amount`, and an `X-Total-Count` header is also handled.
- Errors come back as `{"fault": {"code": ..., "message": ...}}` (WSO2 gateway).
- `links[]` must be found by `rel`: `confirm` (method `REDIRECT`), `complete`, `cancel`, `refund` (`POST`), `self` (`GET`).
- The API needs every decimal written with **exactly two decimals**, and it returns numbers as strings (and vice versa) in `status_code`, `invoice_number`, `transaction_code`, `commission`, amounts and `total`. `items[].name` is a string in payments and an **array of strings in refunds**.
- `merchant_op_id` is documented as "exactly 12 digits". Required fields to create a payment: `return_url, cancel_url, merchant_op_id, currency, amount`.
- It includes a full catalog of transaction status codes (1111–1119) and business error codes (4001–4092, 5000–5004).
- Query parameter names: `limit` (page size), `offset` (page index), `enzona_op_filter` besides `merchant_op_filter`, and `order_filter` with `asc`/`desc`.

### NoxCreation/enzona_api (GPL-3.0, used in production)

- Production URLs: `https://api.enzona.net/payment/v1.0.0/payments`, `/{uuid}/complete`, `/{uuid}/cancel`, `/{uuid}/refund`, `/payments/refunds`.
- It **sends `merchant_uuid`** in the create-payment body, which the generated spec does not list (discrepancy, see coverage).
- Amounts are sent as strings with two decimals (`"10.00"`). A refund without a body is a full refund; `{"amount": {"total": "5.00"}, "description": "..."}` is a partial one.
- It reads `links[0..2]` by position (fragile; this SDK uses `rel`). Errors are read from `fault.message`.
- Refund responses include `refund_name`, `refund_lastname` and `refund_avatar`, and list entries include `transaction_denom` and `amount.details.refunded` / `total_refunded`.

### uicdevs/wp-enzona-api-integration

- Requests one token for several scopes: `scope=enzona_business_payment,enzona_business_qr` (comma-separated, while standard OAuth2 uses spaces).

## Inspiration, what not to copy, and what is confirmed

| Topic | Inspiration | What not to copy | Confirmed? |
|---|---|---|---|
| Base URL and version path | all sources | — | Yes, 4 independent sources |
| OAuth2 client credentials, Basic auth, `/token` | all sources | per-call token requests (NoxCreation, old 1.x) | Yes |
| Scope separator | WordPress plugin (comma) | — | **No**, configurable |
| snake_case bodies, 2-decimal string amounts | Dart, NoxCreation | float arithmetic and string padding | Yes (2 sources) |
| `merchant_uuid` in create body | NoxCreation | — | **Discrepancy**, optional field |
| `links` by `rel` | Dart | positional access (NoxCreation) | Yes |
| Lenient number/string parsing | Dart | — | Yes |
| Pagination block | Dart | the old Swagger shape as the only one | Partially, so modelled leniently |
| Error envelope `fault` | Dart, NoxCreation | printing errors to stdout | Yes |
| Status and error code catalog | Dart | — | Informative, parsed tolerantly |
| Payer-side endpoints | daxslab | — | Out of scope (need the buyer's password) |
| Architecture, build, CI, release | `qvapay-java` (same author) | — | — |

## Discrepancies to resolve with the official Swagger

Record the answers in [api-coverage.md](api-coverage.md):

1. Whether `merchant_uuid` is accepted or required in `POST /payments`.
2. Scope separator in the token request.
3. Exact date format of `start_date_filter` / `end_date_filter`.
4. Whether `offset` is a page index or a record offset.
5. Exact shape of `pagination` in every list.
6. Whether `merchant_op_id` must have exactly 12 digits.
7. Payment expiry time when it is never completed.
8. Semantics of `/payment-orders` and `/payments/checkout/{uuid}`.
9. Status code returned by `POST /payments` (200 or 201). The SDK accepts any 2xx.
