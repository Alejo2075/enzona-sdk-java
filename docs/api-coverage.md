# API coverage

Sources: see [references.md](references.md). The official Swagger lives in the ENZONA API Store, which needs a merchant account. Once exported, it goes into `docs/spec/` and becomes the top source.

**Statuses:** `verified` (contract confirmed by at least two independent sources) → `implemented` → `tested` (offline contract tests with fixtures) → `live-checked` (manually checked in the sandbox). `blocked` means the contract or semantics are unknown. `out-of-scope` means a payer-side operation. `undocumented` means there is no public contract.

Last review of every row: **2026-10-23**.

## Environments and authentication

| Item | Value | Status |
|---|---|---|
| Production base | `https://api.enzona.net` | verified |
| Sandbox base | `https://apisandbox.enzona.net` | verified |
| Token | `POST {base}/token`, `Authorization: Basic base64(key:secret)`, form `grant_type=client_credentials&scope=…` | verified |
| Token response | `access_token`, `token_type`, `expires_in`, `scope`. No refresh token | verified |
| Payment scope | `enzona_business_payment` | verified |
| QR scope | `enzona_business_qr` | verified (2 sources) |
| Scope separator | space (OAuth2) vs comma (WordPress plugin) | `TODO: VERIFY WITH ENZONA`, configurable |
| API calls | `Authorization: Bearer <token>` | verified |

## PaymentAPI v1.0.0 (`{base}/payment/v1.0.0`)

### Merchant operations (release 2.0.0)

| Java operation | Method and path | Request | Response | Status |
|---|---|---|---|---|
| `payments().create(r)` | `POST /payments` | see *Create payment* | `Payment` | tested |
| `payments().get(uuid)` | `GET /payments/{transaction_uuid}` | — | `Payment` | tested |
| `payments().list(q)` | `GET /payments` | `merchant_uuid, limit, offset, merchant_op_filter, enzona_op_filter, status_filter, start_date_filter, end_date_filter, order_filter` | `{"payments": [...], "pagination": {...}}` | tested |
| `payments().complete(uuid)` | `POST /payments/{transaction_uuid}/complete` | empty JSON object `{}` | `Payment` | tested |
| `payments().cancel(uuid)` | `POST /payments/{transaction_uuid}/cancel` | empty JSON object `{}` | `Payment` | tested |
| `refunds().create(uuid, r)` | `POST /payments/{transaction_uuid}/refund` | `{}` (full) or `{"amount": {"total": "5.00"}, "description": "..."}` (partial) | `Refund` | tested |
| `refunds().get(uuid)` | `GET /payments/refund/{transaction_uuid}` | — | `Refund` | tested |
| `refunds().list(q)` | `GET /payments/refunds` | `merchant_uuid, transaction_uuid, commerce_refund_id, limit, offset, status_filter, start_date_filter, end_date_filter, order_filter` | `{"refunds": [...], "pagination": {...}}` | tested |
| `refunds().listForPayment(uuid, q)` | `GET /payments/{transaction_uuid}/refunds` | `limit, offset, status_filter, start_date_filter, end_date_filter, order_filter` | `{"refunds": [...], "pagination": {...}}` | tested |

### Create payment

```json
{
  "merchant_uuid": "optional, see discrepancy 1",
  "description": "Order #123",
  "currency": "CUP",
  "amount": {"total": "112.00", "details": {"shipping": "10.00", "tax": "2.00", "discount": "0.00", "tip": "0.00"}},
  "items": [{"name": "T-shirt", "description": "Blue", "quantity": 2, "price": "50.00", "tax": "2.00"}],
  "merchant_op_id": "123456789012",
  "invoice_number": "1001",
  "return_url": "https://shop.example/enzona/return",
  "cancel_url": "https://shop.example/enzona/cancel",
  "terminal_id": "12",
  "buyer_identity_code": ""
}
```

- Required (Dart SDK): `return_url`, `cancel_url`, `merchant_op_id`, `currency`, `amount`.
- Amounts are strings with exactly two decimals.
- Server-side checks: `4083` (the sum of item `tax` differs from `details.tax`) and `4084` (the details do not add up to `total`). Formula: `total = Σ(price × quantity) + tax + shipping + tip − discount`.

### Response fields (`Payment`, `Refund`)

Every field is optional on read. Observed fields: `transaction_uuid, transaction_code, transaction_signature, transaction_denom, transaction_description, transaction_created_at, transaction_updated_at, currency, created_at, update_at, updated_at, status_code, status_denom, description, invoice_number, merchant_op_id, terminal_id, amount{total, details{shipping, tax, discount, tip, refunded, total_refunded}}, items[], links[]{rel, method, href}, commission, buyer_identity_code, merchant_uuid, merchant_name, merchant_alias, merchant_avatar, refunded_amount, username, name, lastname, avatar, return_url, cancel_url`. Refunds also have `transaction_status_code, parent_payment_uuid, refund_name, refund_lastname, refund_avatar, commerce_refund_id`.

Known type quirks, all handled by the codec:

| Field | Seen as |
|---|---|
| `status_code`, `transaction_code`, `invoice_number`, `terminal_id`, `pagination.total` | number **or** string |
| amounts, `commission`, `total_amount` | string **or** number |
| `items[].name` | string (payments) **or** array of strings (refunds) |
| `status_denom` | string **or** array of strings |
| `update_at` / `updated_at` | both spellings exist |

### Transaction status codes

| Code | Meaning |
|---|---|
| 1111 | accepted |
| 1112 | failed |
| 1113 | pending |
| 1114 | reversed |
| 1115 | refunded |
| 1116 | confirmed |
| 1117 | cancelled |
| 1118 | activated |
| 1119 | deactivated |

### Error envelopes

| Origin | Body | Examples |
|---|---|---|
| WSO2 gateway | `{"fault": {"code": 900901, "message": "Invalid Credentials", "description": "..."}}` | 900901 invalid credentials, 900902 missing credentials, 900800/900802 throttled |
| ENZONA API | `{"code": 4064, "message": "..."}` | 4064 transaction not found, 4065 payment to refund not confirmed, 4067 already fully refunded, 4068/4069 refund exceeds the payment, 4070 zero refund, 4073 checkout not found, 4082–4084 malformed payment, 4092 bad data structure, 5001–5003 system or bank failure |

The gateway codes are WSO2 defaults and should be confirmed in the sandbox.

### Not implemented

| Method and path | Body | Status | Reason |
|---|---|---|---|
| `POST /payments/{uuid}/confirm` | `payment_password, funding_source_uuid` | out-of-scope | Payer-side: needs the buyer's payment password |
| `POST /payments/store` | `funding_source_uuid, description, currency, payment_password, amount{total}, items[]{vendor_identity_code, product_id, quantity, price}` | out-of-scope | Payer-side |
| `POST /payments/vendor/code` | `funding_source_uuid, amount, vendor_identity_code, description, currency, payment_password` | out-of-scope | Payer-side |
| `POST /payment-orders` | `description, currency, amount, merchant_op_id` | blocked | Unknown semantics |
| `GET /payments/checkout/{uuid}` | — (empty response) | blocked | Unknown semantics |

## QR API v1.0.0 (`{base}/qr/v1.0.0`, planned for 2.1.0)

| Method and path | Request | Response | Status |
|---|---|---|---|
| `POST /qr/merchant` | `amount, currency, description, return_url, notify_url` | `vendor_identity_code, create_at, update_at, image` | verified (spec only) |
| `GET /qr/{qr_code}` | — | `qr_code, amount, currency, description, name, avatar, require_password, username` | verified (spec only) |

`notify_url` implies server notifications, but there is no known signature scheme, so the SDK will not offer a verifier until one is documented.

## Other APIs

| API | Status |
|---|---|
| Claim API | undocumented |
| XMET Account / AccountingOperation | undocumented |

## Open questions (`TODO: VERIFY WITH ENZONA`)

1. `merchant_uuid` in `POST /payments`: accepted, required or ignored?
2. Separator between several scopes in the token request.
3. Format of `start_date_filter` / `end_date_filter`. The SDK sends `yyyy-MM-dd`.
4. `offset`: page index (Dart SDK) or record offset?
5. Shape of `pagination` (Dart: `first/prev/next/last/total/total_amount`; old Swagger: `current_page/total_page/page_size/total/links`).
6. `merchant_op_id`: exactly 12 digits?
7. Expiry of a payment that is never completed.
8. HTTP status of `POST /payments` (200 or 201). Any 2xx is accepted.
9. `commerce_refund_id` and `username` in the refund body (seen in the 1.x SDK only).
