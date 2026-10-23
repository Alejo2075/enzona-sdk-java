# Migrating from 1.x to 2.0.0

`2.0.0` is a ground-up rewrite. There is no compatibility layer: the package, the classes and the way you call the SDK changed. The previous code stays reachable through the Git tag `legacy-1.0`.

## Why

The 1.x line could not work against the live API:

- It called `https://api.enzona.net/payments` instead of `https://api.enzona.net/payment/v1.0.0/payments`.
- It sent camelCase JSON (`merchantUuid`, `returnUrl`) where ENZONA expects snake_case (`merchant_uuid`, `return_url`).
- Because unknown fields were ignored, responses were deserialized to objects full of `null` without any error.
- `payProduct` called `/shop` instead of `/payments/store`.
- It requested a new access token for every call, never closed its HTTP clients and printed the consumer secret in `toString()`.
- It had no tests.

## What changed

| 1.x | 2.0.0 |
|---|---|
| package `io.github.alejo2075.enzonasdk` | `io.github.alejo2075.enzona` |
| `new PaymentApi(key, secret)` | `EnzonaClient.builder().credentials(key, secret).build()` then `client.payments()` / `client.refunds()` |
| fixed production URL | `Environment.SANDBOX` / `PRODUCTION` |
| `double` amounts | `BigDecimal`, written as `"10.00"`, never rounded |
| checked and unchecked exceptions mixed | unchecked `EnzonaException` hierarchy with ENZONA codes |
| a token per call | cached token, renewed once when the gateway rejects it |
| no async | every operation has an `...Async` form |

## Method equivalents

| 1.x | 2.0.0 |
|---|---|
| `PaymentApi.createPayment(request)` | `client.payments().create(CreatePaymentRequest.builder()…build())` |
| `PaymentApi.getPaymentDetails(uuid)` | `client.payments().get(uuid)` |
| `PaymentApi.getPaymentsList(request)` | `client.payments().list(PaymentQuery…)` |
| `PaymentApi.completePayment(uuid)` | `client.payments().complete(uuid)` |
| `PaymentApi.cancelPayment(uuid)` | `client.payments().cancel(uuid)` |
| `PaymentApi.refundPayment(uuid, request)` | `client.refunds().create(uuid, RefundRequest…)` |
| `PaymentApi.getRefundDetails(uuid)` | `client.refunds().get(uuid)` |
| `PaymentApi.getRefundsList(request)` | `client.refunds().list(RefundQuery…)` |
| `PaymentApi.listRefunds(request)` | `client.refunds().listForPayment(uuid, RefundQuery…)` |
| `PaymentApi.performCheckout`, `createPaymentOrder` | not included: semantics unknown (see [api-coverage.md](api-coverage.md)) |
| `PaymentApi.confirmPayment`, `payProduct`, `createReceiveCode` | not included: they are done by the **buyer** with their payment password, not by a merchant backend |
| `ClaimApi.createClaims` | not included: no public contract |

## Example

```java
// 1.x
PaymentApi api = new PaymentApi("key", "secret");
CreatePaymentRequest r = new CreatePaymentRequest();
r.setCurrency("CUP");
r.setMerchantOpId(123456789012L);
// … many setters …
CreatePaymentResponse response = api.createPayment(r);

// 2.0.0
EnzonaClient client = EnzonaClient.builder().credentials("key", "secret").build();
Payment payment = client.payments().create(
        CreatePaymentRequest.builder()
                .currency("CUP")
                .item(PaymentItem.of("T-shirt", 2, new BigDecimal("50.00"), new BigDecimal("2.00")))
                .merchantOpId("123456789012")
                .returnUrl("https://shop.example/return?order=1")
                .cancelUrl("https://shop.example/cancel?order=1")
                .build());
```

## Behaviour you must adopt

- Store `transactionUuid` with your order: ENZONA does not put it in your return URL.
- On the buyer's return, `get` the payment, verify it and then `complete` it ([payment-flow.md](payment-flow.md)).
- Handle the numeric `errorCode()` instead of parsing messages.
- Change the artifact version, keep `groupId` and `artifactId`: `io.github.alejo2075:enzona-sdk`.
