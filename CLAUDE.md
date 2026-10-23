# CLAUDE.md — ENZONA Java SDK

> **Misión:** reescribir desde cero, documentar, probar y dejar **listo para publicar en Maven Central** un SDK Java moderno, tipado, seguro y sin acoplamiento a frameworks para la API de pagos de **ENZONA** (Cuba).
>
> Instrucciones operativas para Claude Code (o cualquier agente). **Investigación de referencia:** 23 de octubre de 2026. **Idioma:** este archivo y `PLAN.md` en español; código, Javadoc, README, `docs/` y mensajes de commit en **inglés** (`README.es.md` es la traducción). **Plan de ejecución:** [`PLAN.md`](PLAN.md). **Proyecto hermano y modelo a seguir:** `qvapay-java` (mismo autor, ya publicado en Central).

## 1. Contexto

- Repositorio: `https://github.com/Alejo2075/enzona-sdk-java`, rama `main`, público.
- Existe un SDK antiguo publicado como `io.github.alejo2075:enzona-sdk:0.9.5` y `:1.0` (mayo 2024). **Está roto**: URL base sin `/payment/v1.0.0`, JSON en camelCase en vez de snake_case (las respuestas se deserializan en silencio a `null`), `/shop` en vez de `/payments/store`, token pedido en cada llamada, `HttpClient` sin cerrar, `@Data` que imprime el `consumerSecret`, sin tests. **No se conserva nada del código antiguo**: se reescribe.
- Las versiones publicadas son inmutables. La reescritura se publica como **`2.0.0`** (cambio incompatible: nuevo paquete y nueva API). Desarrollo en `2.0.0-SNAPSHOT` (solo local, nunca se publica).
- ENZONA es la plataforma de pagos de **XETID** (Cuba). Este SDK es **comunitario/no oficial**; no afirmar afiliación con ENZONA, XETID ni los bancos.

### Principios no negociables

1. **No inventar** endpoints, campos, códigos ni semánticas. Lo que no esté confirmado se marca `TODO: VERIFY WITH ENZONA` en el código y se registra en `docs/api-coverage.md`.
2. Dinero con `BigDecimal`, serializado como **string con exactamente 2 decimales** (`"10.00"`), que es lo que exige la API. Nunca `double`.
3. Seguridad por defecto: HTTPS, secretos fuera del código y de logs/excepciones/`toString()`, sin TLS inseguro, sin seguir redirecciones con credenciales.
4. **Nunca reintentar** automáticamente operaciones que mueven dinero (`create`, `complete`, `cancel`, `refund`). Solo lecturas, como opt-in.
5. Ningún test ni ejemplo usa dinero real ni toca la red de ENZONA. La integración real es manual y en **sandbox**.
6. JSON real por endpoint (snake_case, tipos inconsistentes de la API tolerados al leer); Jackson es detalle interno y no aparece en la API pública.
7. Código original. Las referencias se estudian, no se copian (ver licencias en §2).

## 2. Fuentes de verdad

ENZONA **no publica documentación abierta indexable**: la referencia oficial está en el **API Store de WSO2** (`https://api.enzona.net/store/`, sandbox `https://apisandbox.enzona.net/store/`), al que se accede tras registrar el comercio en `https://bulevar.enzona.net`. Desde fuera de Cuba esos hosts **suelen dar timeout** (comprobado el 23-oct-2026), así que el contrato se reconstruye a partir de clientes generados con el Swagger oficial y de integraciones que se usaron en producción.

| Prioridad | Fuente | Qué aporta | Licencia / restricción |
|---|---|---|---|
| 1 | API Store oficial (`api.enzona.net/store`, `apisandbox.enzona.net/store`) | Swagger real de PaymentAPI v1.0.0, QR API v1.0.0, etc. | Solo accesible con cuenta de comercio; **el mantenedor debe exportar el Swagger y dejarlo en `docs/spec/`** (HUMAN SETUP) |
| 2 | [daxslab/enzona-payment-python](https://github.com/daxslab/enzona-payment-python) / [-php](https://github.com/daxslab/enzona-payment-php) | Generados con swagger-codegen del Swagger oficial (2019-2020): 14 rutas, parámetros y modelos de request | PHP: MIT (`composer.json`); Python: sin LICENSE → solo lectura |
| 3 | [daxslab/enzona-qr-python](https://github.com/daxslab/enzona-qr-python) | QR API: `POST /qr/merchant`, `GET /qr/{qr_code}`, scope `enzona_business_qr` | Sin LICENSE → solo lectura |
| 4 | [luis901101/enzona](https://github.com/luis901101/enzona) (Dart, pub.dev `enzona`) | Lo más completo sobre **respuestas reales**: campos, `links` por `rel`, paginación, catálogo de códigos `status_code`/errores 4xxx, rarezas de tipos, importes a 2 decimales, sin refresh token | BSD-style → atribuir si se reutiliza algo |
| 5 | [NoxCreation/enzona_api](https://github.com/NoxCreation/enzona_api) (Python, 2022) | Uso real en producción: rutas con `/payment/v1.0.0`, cuerpos con `merchant_uuid`, listas `{"payments":[...]}` / `{"refunds":[...]}`, errores `{"fault":{...}}` | **GPL-3.0 → NO copiar ni traducir código** |
| 6 | [uicdevs/wp-enzona-api-integration](https://github.com/uicdevs/wp-enzona-api-integration) | Token con varios scopes `enzona_business_payment,enzona_business_qr` | Sin LICENSE → solo lectura |
| — | `qvapay-java` (`~/WebstormProjects/qvapay-java`, `github.com/Alejo2075/qvapay-java`) | **Modelo de arquitectura, build, CI, release, docs y estrategia de commits** | Del mismo autor: se puede reutilizar código y adaptarlo |

Orden de precedencia para **comportamiento de red**: Swagger oficial exportado > respuestas reales capturadas en sandbox > clientes generados con Swagger (daxslab) > clientes usados en producción (Dart, NoxCreation) > hipótesis. Registrar en `docs/api-coverage.md` fuente y fecha por endpoint, y anotar las discrepancias (p. ej. `merchant_uuid` no aparece en el Swagger de daxslab, pero NoxCreation lo envía).

Otras referencias: Central Portal (https://central.sonatype.org/publish/publish-portal-maven/), requisitos (https://central.sonatype.org/publish/requirements/).

## 3. Autenticación y entornos

| Entorno | Base API | Token |
|---|---|---|
| Producción | `https://api.enzona.net` | `POST https://api.enzona.net/token` |
| Sandbox | `https://apisandbox.enzona.net` | `POST https://apisandbox.enzona.net/token` |

- Gateway **WSO2 API Manager**, OAuth2 **client credentials**:
  ```http
  POST /token
  Authorization: Basic base64(consumer_key:consumer_secret)
  Content-Type: application/x-www-form-urlencoded

  grant_type=client_credentials&scope=enzona_business_payment enzona_business_qr
  ```
  Respuesta: `{"access_token":"...","scope":"...","token_type":"Bearer","expires_in":3600}`. **No hay refresh token**: se pide otro token.
- Consumer key/secret se obtienen por aplicación en el API Store (claves distintas para sandbox y producción).
- Scopes conocidos: `enzona_business_payment` (PaymentAPI) y `enzona_business_qr` (QR API). El separador entre scopes es el espacio según OAuth2; la integración de WordPress usó coma → `TODO: VERIFY WITH ENZONA`, y el SDK debe dejarlo configurable.
- Llamadas a la API: `Authorization: Bearer <token>`, `Accept: application/json`, `Content-Type: application/json` cuando hay body.
- **Diseño:** `OAuthTokenProvider` interno que guarda el token en caché hasta `expires_in - margen` (60 s por defecto), es seguro con concurrencia (una sola petición de token a la vez), usa `Clock` inyectable y permite inyectar un `AccessTokenProvider` propio. Ante un `401` de la API se invalida el token, se pide uno nuevo y se repite la petición **una única vez**. Esto vale incluso para operaciones de dinero, porque el 401 lo devuelve el gateway antes de llegar al backend; documentarlo y probarlo.
- El secreto y el token nunca aparecen en `toString()`, mensajes de excepción ni logs.

## 4. Contrato de la PaymentAPI v1.0.0

Base: `{env}/payment/v1.0.0`. Todas las rutas usan Bearer con scope `enzona_business_payment`.

### 4.1 Operaciones del comercio (alcance de `2.0.0`)

| Operación Java propuesta | Método y ruta | Entrada | Salida |
|---|---|---|---|
| `payments().create(req)` | `POST /payments` | `CreatePaymentRequest` | `Payment` con `transaction_uuid`, `status_code`, `links` |
| `payments().get(uuid)` | `GET /payments/{transaction_uuid}` | — | `Payment` |
| `payments().list(query)` | `GET /payments` | query: `merchant_uuid`, `limit`, `offset`, `merchant_op_filter`, `enzona_op_filter`, `status_filter`, `start_date_filter`, `end_date_filter`, `order_filter` (`asc`/`desc`) | `PaymentPage` (`payments` + `pagination`) |
| `payments().complete(uuid)` | `POST /payments/{uuid}/complete` | body vacío | `Payment` |
| `payments().cancel(uuid)` | `POST /payments/{uuid}/cancel` | body vacío | `Payment` |
| `refunds().create(uuid, req)` | `POST /payments/{uuid}/refund` | `{"amount":{"total":"5.00"},"description":"..."}` (sin `amount` = devolución total; también `commerce_refund_id`, `username`, `TODO: VERIFY`) | `Refund` |
| `refunds().get(uuid)` | `GET /payments/refund/{transaction_uuid}` | — | `Refund` |
| `refunds().list(query)` | `GET /payments/refunds` | `merchant_uuid`, `transaction_uuid`, `commerce_refund_id`, `limit`, `offset`, `status_filter`, `start_date_filter`, `end_date_filter`, `order_filter` | `RefundPage` (`refunds` + `pagination`) |
| `refunds().listForPayment(uuid, query)` | `GET /payments/{uuid}/refunds` | `limit`, `offset`, `status_filter`, fechas, `order_filter` | `RefundPage` |

**Crear pago**, request (snake_case, importes como string con 2 decimales):

```json
{
  "merchant_uuid": "…",
  "description": "Order #123",
  "currency": "CUP",
  "amount": { "total": "112.00",
              "details": { "shipping": "10.00", "tax": "2.00", "discount": "0.00", "tip": "0.00" } },
  "items": [ { "name": "T-shirt", "description": "Blue", "quantity": 2, "price": "50.00", "tax": "2.00" } ],
  "merchant_op_id": "123456789012",
  "invoice_number": "1001",
  "return_url": "https://shop.example/enzona/return",
  "cancel_url": "https://shop.example/enzona/cancel",
  "terminal_id": "12",
  "buyer_identity_code": ""
}
```

- Requeridos según el SDK Dart: `return_url`, `cancel_url`, `merchant_op_id`, `currency`, `amount`. `merchant_uuid` lo envían las integraciones reales, pero no está en el Swagger de daxslab → opcional y `TODO: VERIFY`.
- `merchant_op_id`: identificador del comercio para la operación, **12 dígitos** según el SDK Dart (`TODO: VERIFY`). Validarlo en el builder solo si se confirma; si no, documentarlo.
- Validaciones de servidor conocidas: `4083` (la suma del `tax` de los items no coincide con `details.tax`), `4084` (la suma de los detalles no coincide con `total`). Fórmula: `total = Σ(price × quantity) + tax + shipping + tip − discount`, con `tax = Σ items.tax`. Ofrecer un helper `PaymentAmount.computeFrom(items, shipping, tip, discount)` y validar en el builder (opción desactivable) **sin redondear en silencio**: los importes con más de 2 decimales se rechazan.
- Monedas observadas: `CUP` (y `USD` según las noticias). Representar `currency` como `String` validado (3 letras), no como enum cerrado.

**Respuesta `Payment`** (campos observados; todos opcionales al leer): `transaction_uuid`, `transaction_code`, `transaction_signature`, `transaction_denom`, `transaction_description`, `transaction_created_at`, `transaction_updated_at`, `currency`, `created_at`, `update_at` (sic), `updated_at`, `status_code`, `status_denom`, `description`, `invoice_number`, `merchant_op_id`, `terminal_id`, `amount{total, details{shipping,tax,discount,tip}}`, `items[]`, `links[]{rel,method,href}`, `commission`, `buyer_identity_code`, `merchant_uuid`, `merchant_name`, `merchant_alias`, `merchant_avatar`, `refunded_amount`, `username`, `name`, `lastname`, `avatar`, `return_url`, `cancel_url`.

**Rarezas que el códec debe tolerar** (documentadas en el SDK Dart): números que llegan como string y al revés (`status_code`, `invoice_number`, `terminal_id`, importes); `items[].name` es string en pagos y **array de strings en devoluciones**; `status_denom` a veces llega como array. Deserializar de forma tolerante con deserializadores dedicados, cubiertos con fixtures.

**`links` por `rel`** (nunca por posición): `confirm` (`REDIRECT`, URL a la que se envía al comprador), `complete`, `cancel`, `refund` (`POST`, se ejecutan con el SDK y no siguiendo el `href`), `self` (`GET`). Exponer `payment.confirmationUrl()` como `Optional<URI>`.

**Paginación**: el cuerpo trae `pagination` con `first`, `prev`, `next`, `last`, `total` y `total_amount`; puede existir la cabecera `X-Total-Count`. El Swagger antiguo describe otra forma (`current_page`, `total_page`, `page_size`, `links.first_page.href`…). Modelar `PageInfo` de forma tolerante y no inventar campos. `limit`/`offset` son números; `offset` se comporta como índice de página según el SDK Dart (`TODO: VERIFY`).

**Fechas de filtro**: formato exacto sin confirmar (`TODO: VERIFY`). Aceptar `LocalDate`/`OffsetDateTime` en la API Java y centralizar el formateo en un único sitio.

### 4.2 Flujo de pago (no hay webhooks)

```
create (status 1113 pendiente) → redirigir al comprador a links[rel=confirm]
  → el comprador paga en ENZONA → redirección a return_url?transaction_uuid=…  (o a cancel_url)
  → el backend hace GET /payments/{uuid} y comprueba importe, merchant_op_id y estado
  → POST /payments/{uuid}/complete (status 1111 aceptada)    | o cancel
```

- La PaymentAPI **no tiene webhooks firmados**: la redirección a `return_url` **no es prueba de pago**. El backend siempre consulta el pago y llama a `complete`. Si no se completa, el pago no se cobra (`TODO: VERIFY` el tiempo de expiración).
- El SDK no aloja endpoints HTTP. Sí aporta un helper `ReturnUrlParser` para extraer `transaction_uuid` de la query (no valida nada por sí mismo) y documentación del flujo.

### 4.3 Códigos de estado y de error

- `status_code` de transacción: `1111` aceptada, `1112` fallida, `1113` pendiente, `1114` reversada, `1115` devuelta, `1116` confirmada, `1117` cancelada, `1118` activada, `1119` desactivada. `enum PaymentStatus` con `UNKNOWN` y conservando el valor crudo.
- Errores de negocio `4xxx`/`5xxx` (catálogo del SDK Dart, p. ej. `4064` transacción inexistente, `4065` pago a devolver no confirmado, `4067` devuelto en su totalidad, `4068`/`4069` el importe excede el devolvible, `4070` importe cero, `4073` checkout inexistente, `4082`–`4084` estructura de pago incorrecta, `4092` error de estructura, `5001`–`5003` fallo del sistema/banco). Tabla `EnzonaErrorCode` informativa y tolerante; **la lógica actúa sobre el código numérico, nunca sobre el texto**.
- Cuerpo de error del gateway WSO2: `{"fault":{"code":900901,"message":"Invalid Credentials","description":"…"}}` (códigos 9009xx = auth/throttling). Cuerpo de error de la API: `{"code":4064,"message":"…"}`. El parser acepta ambos y conserva el cuerpo crudo **sin cabeceras de petición**.
- Excepciones (unchecked) a partir de `EnzonaException`: `EnzonaApiException` (status HTTP, `errorCode`, `message`, `description`) → `ValidationException` 400, `AuthenticationException` 401, `PermissionDeniedException` 403, `NotFoundException` 404, `RateLimitException` 429 (`retryAfter`), `ServerException` 5xx. Además `TransportException`, `SerializationException` y `TokenException` (fallo al obtener el token).

### 4.4 Fuera del alcance de 2.0.0 (registrar en `api-coverage.md`)

| Ruta | Motivo |
|---|---|
| `POST /payments/{uuid}/confirm` (`payment_password`, `funding_source_uuid`) | Operación **del pagador** con su contraseña de pago; no corresponde a un backend de comercio |
| `POST /payments/store` (`funding_source_uuid`, `payment_password`, items con `vendor_identity_code`) | Ídem (pago de comprador en tienda Bulevar) |
| `POST /payments/vendor/code` (`payment_password`, `vendor_identity_code`) | Ídem |
| `POST /payment-orders` (`amount`, `currency`, `description`, `merchant_op_id`) | Semántica desconocida → `blocked` hasta tener el Swagger |
| `GET /payments/checkout/{uuid}` (respuesta vacía) | Semántica desconocida → `blocked` |
| QR API v1.0.0: `POST /qr/merchant` (`amount`, `currency`, `description`, `return_url`, `notify_url`), `GET /qr/{qr_code}`; scope `enzona_business_qr` | Fase 2 (`2.1.0`). `notify_url` sugiere notificaciones: **no implementar verificación sin contrato de firma** |
| Claim API, XMET Account/AccountingOperation | Sin contrato público → `undocumented` |

## 5. Stack y arquitectura

Mismo baseline que `qvapay-java` (verificado el 22/23-oct-2026; volver a comprobarlo antes de la release):

| Componente | Versión |
|---|---|
| JDK de desarrollo / CI principal | **17 LTS** (`~/.jdks/jdk17`, Temurin 17.0.21); compatibilidad con **21** (`~/.jdks/jdk21`) y 25 opcional en CI |
| Bytecode | `maven.compiler.release=17` (major 61); prohibido usar APIs de Java 18+ |
| Maven Wrapper | 3.3.4 (script) → Maven **3.10.0**; nunca Maven 4 para publicar |
| compiler / surefire / jar / source / javadoc / enforcer | 3.16.0 / 3.6.0 / 3.5.1 / 3.4.0 / 3.12.0 / 3.6.3 |
| clean / resources / install / deploy | 3.5.0 / 3.5.0 / 3.2.0 / 3.2.0 |
| Spotless + google-java-format (AOSP) | 3.10.4 + **1.27.0** (la última que corre en JDK 17) |
| JaCoCo | 0.8.15 (umbral final: 90 % líneas, 80 % ramas) |
| JSON | **Jackson 3.2.4** (`tools.jackson:jackson-bom`, `tools.jackson.core:jackson-databind`; anotaciones `com.fasterxml.jackson.annotation`) |
| Tests | **JUnit 6.1.3** (BOM), servidor `com.sun.net.httpserver` del JDK + transporte falso en memoria (cero dependencias extra) |
| Publicación | `central-publishing-maven-plugin` **0.12.0** (`autoPublish=false`) + `maven-gpg-plugin` **3.2.8**, solo en el perfil `central` |
| Ejemplos Gradle | Gradle Wrapper **9.8.2**, toolchain 17 |

La máquina no tiene `java` en el PATH: exportar `JAVA_HOME=~/.jdks/jdk17` (o `jdk21`) y usar siempre `./mvnw`.

### Coordenadas y paquete

- `groupId` **`io.github.alejo2075`** (namespace ya verificado en Central Portal, el mismo de `qvapay-java`).
- `artifactId` **`enzona-sdk`** (se mantiene para dar continuidad a los usuarios de 1.0), versión **`2.0.0`**.
- Paquete raíz **`io.github.alejo2075.enzona`**, `Automatic-Module-Name: io.github.alejo2075.enzona`.
- Licencia **Apache-2.0** (la del proyecto publicado; renombrar `LICENSE.txt` a `LICENSE` con el texto completo y © 2024-2026 Alejandro Santana).
- POM: `name` "ENZONA Java SDK", `url`/`scm` `https://github.com/Alejo2075/enzona-sdk-java` (rama `main`), developer `Alejo2075` / Alejandro Santana / `https://github.com/Alejo2075`. **Quitar** la URL inventada de desoft.cu y la organización "Desoft".

### Layout

```text
enzona-sdk-java/
├── CLAUDE.md  PLAN.md  README.md  README.es.md  CHANGELOG.md  LICENSE  NOTICE (si aplica)
├── CONTRIBUTING.md  CODE_OF_CONDUCT.md  SECURITY.md
├── pom.xml  mvnw  mvnw.cmd  .mvn/wrapper/
├── .editorconfig  .gitattributes  .gitignore  .env.example
├── .github/  workflows/{ci,release,pages}.yml  dependabot.yml  ISSUE_TEMPLATE/  pull_request_template.md
├── docs/  api-coverage.md  references.md  architecture.md  payment-flow.md  security.md
│          troubleshooting.md  release.md  migration-1.x.md  spec/ (Swagger exportado, si existe)  assets/
├── scripts/  readme_snippets.py  verify_bundle.py        (adaptados de qvapay-java)
├── examples/  plain-java/  gradle-java/  gradle-groovy/  return-handler/
└── src/main/java/io/github/alejo2075/enzona/
    ├── EnzonaClient.java  Environment.java  EnzonaVersion.java  PaymentsApi.java  RefundsApi.java
    ├── auth/       ClientCredentials, AccessTokenProvider, OAuthTokenProvider, Scope
    ├── http/       HttpTransport, JdkHttpTransport, ApiRequest, ApiResponse, RetryPolicy
    ├── internal/   RequestExecutor, JsonCodec, ErrorMapper, UrlEncoding, Validation, Money, ClientSettings
    ├── model/      Payment, CreatePaymentRequest, PaymentAmount, AmountDetails, PaymentItem, PaymentLink,
    │               PaymentStatus, PaymentQuery, PaymentPage, Refund, RefundRequest, RefundQuery, RefundPage,
    │               PageInfo, SortOrder
    ├── flow/       ReturnUrlParser
    └── exception/  EnzonaException y subclases, EnzonaErrorCode
```

Se elimina del repositorio: `docs/` (Javadoc generado y versionado), `.idea/` versionado, `src/` antiguo y `LICENSE.txt`.

### Superficie pública (diseño propuesto)

```java
EnzonaClient client = EnzonaClient.builder()
    .credentials(System.getenv("ENZONA_CONSUMER_KEY"), System.getenv("ENZONA_CONSUMER_SECRET"))
    .environment(Environment.SANDBOX)            // PRODUCTION por defecto
    .connectTimeout(Duration.ofSeconds(10))
    .requestTimeout(Duration.ofSeconds(30))
    .build();

Payment payment = client.payments().create(
    CreatePaymentRequest.builder()
        .merchantOpId("123456789012")
        .currency("CUP")
        .description("Order #123")
        .item(PaymentItem.of("T-shirt", 2, new BigDecimal("50.00"), new BigDecimal("2.00")))
        .shipping(new BigDecimal("10.00"))
        .returnUrl(URI.create("https://shop.example/enzona/return"))
        .cancelUrl(URI.create("https://shop.example/enzona/cancel"))
        .build());                               // calcula y valida amount.total

URI redirect = payment.confirmationUrl().orElseThrow();
// … el comprador vuelve a return_url …
Payment fresh = client.payments().get(payment.transactionUuid());
Payment done  = client.payments().complete(payment.transactionUuid());
```

Convenciones: `record` inmutables para respuestas; clases finales con builder para requests (validan en `build()`); nombres Java en camelCase y JSON snake_case con `@JsonProperty`; cada operación tiene su gemela `...Async` que devuelve `CompletableFuture` (como en qvapay-java); `EnzonaClient` es seguro entre hilos y se reutiliza (no es `AutoCloseable`: `HttpClient` de JDK 17 no tiene `close()`).

### Comportamiento HTTP

- `User-Agent: enzona-sdk-java/<version>`, leído de un recurso filtrado por Maven.
- HTTPS obligatorio; `baseUrl` personalizada solo con `allowInsecureBaseUrlForTesting()` para loopback en tests. `Redirect.NEVER`.
- Codificar segmentos de ruta y parámetros de query (un `transaction_uuid` con `/` no debe poder cambiar la ruta).
- Cualquier `2xx` es éxito. Errores según §4.3. Conservar la interrupción del hilo (`Thread.currentThread().interrupt()`).
- Reintentos desactivados por defecto. `RetryPolicy` opt-in solo para `GET`, con backoff exponencial, jitter y respeto de `Retry-After`. Nunca para `POST`. El único caso aparte es la repetición tras un 401 de token (§3).

## 6. Seguridad

- Secretos por variables de entorno o gestor de secretos; `.env` en `.gitignore`; `.env.example` sin valores.
- Nunca registrar consumer secret, token, `payment_password` ni cuerpos con datos personales.
- No enviar el Bearer a URLs de `links` (el `href` de `confirm` es para el navegador del comprador).
- El backend nunca da un pago por bueno por la redirección: consulta el pago, compara `amount.total`, `currency` y `merchant_op_id` con su pedido, y después ejecuta `complete`. Procesamiento idempotente por `transaction_uuid`.
- `merchant_op_id`/`commerce_refund_id` **no** son claves de idempotencia mientras no se confirme.

## 7. Pruebas y criterios de aceptación

Unitarias y de contrato, **sin red real**:

- [ ] Petición de token: Basic auth exacto, form-urlencoded, scopes; caché, expiración con `Clock` falso, concurrencia (una sola petición de token con N hilos), repetición única tras un 401, error de token → `TokenException` sin secretos.
- [ ] Método, ruta (`/payment/v1.0.0/...`) y query exactos de cada operación; encoding de segmentos.
- [ ] Serialización snake_case e importes `"10.00"` (y `new BigDecimal("10")` → `"10.00"`; más de 2 decimales → error).
- [ ] Validación y cálculo del total (casos 4083/4084 detectados en cliente).
- [ ] Deserialización tolerante: números como string, `items[].name` como array, `status_denom` como array, campos desconocidos, `null`.
- [ ] `links` por `rel` en cualquier orden; `confirmationUrl()` vacío si no viene.
- [ ] Listados con `pagination` y con la forma antigua; lista vacía.
- [ ] Errores: `fault` WSO2, `{code,message}`, cuerpo vacío o no JSON; 400/401/403/404/429/500; `Retry-After`.
- [ ] `complete`/`cancel`/`create`/`refund` no se reintentan ante 5xx/429/timeout; las lecturas con `RetryPolicy` sí.
- [ ] Ningún `toString()`, excepción ni log contiene el secreto o el token; aislamiento entre dos clientes con credenciales distintas.
- [ ] Paridad sync/async y convivencia con Jackson 2 en el classpath.

Integración (manual, **nunca en CI**, solo sandbox y con aprobación): token, crear pago, abrir la URL de confirmación, completar o cancelar, consultar y listar, devolución parcial en sandbox. Registrar las diferencias en `docs/api-coverage.md` y guardar las respuestas reales anonimizadas como fixtures.

### Definition of Done de 2.0.0

1. `./mvnw -B -ntp clean verify` en verde con JDK 17 y 21 (formato, tests, JaCoCo ≥ 90/80, Javadoc sin warnings, enforcer).
2. Las 9 operaciones de §4.1, tipadas, documentadas y probadas con fixtures.
3. Ejemplos Maven y Gradle que consumen el JAR instalado y pasan contra un servidor simulado.
4. README EN/ES con fragmentos que compilan (`scripts/readme_snippets.py check`), guía de migración desde 1.x, `docs/*` completos.
5. CI, Dependabot, gitleaks y release manual protegida; bundle firmado verificado con `scripts/verify_bundle.py` **sin subir nada**.
6. Sin secretos, sin dinero real, sin afirmar que está publicado antes de comprobarlo en Central.

## 8. README (requisito de primera clase)

Seguir la estructura del README de `qvapay-java`: un hero centrado ("ENZONA Java SDK — a lightweight, type-safe Java client for the ENZONA payment API", aviso **Unofficial / community maintained**), *Why this SDK*, tabla de *Features* con versión y estado, *Requirements*, *Installation* (Maven, Gradle Groovy y Kotlin; mientras no se publique, `Status: pre-release` y nada de coordenadas falsas), *Quick start* (≤ 30 líneas, sacado de `examples/plain-java`), *Payment flow* (Mermaid create → confirm → return_url → get → complete), *Refunds*, *Error handling*, *Security*, *Migrating from 1.x*, *Compatibility*, índice de documentación, *Contributing / Roadmap / License / Disclaimer*. Los badges deben ser reales: Maven Central solo cuando 2.0.0 sea visible; nada de badges falsos ni enlaces rotos.

## 9. Publicación en Maven Central (mismo sitio que qvapay-java)

- Mismo **Central Portal** (`https://central.sonatype.com`), mismo namespace `io.github.alejo2075` (ya verificado), misma clave GPG `BC2FB2385127DC8345884595564BA523FA3BCCF6` (caduca el 22-oct-2028) y el mismo flujo: perfil `central`, `autoPublish=false`, publicación manual en *Deployments*.
- **No** usar el flujo antiguo OSSRH/`s01.oss.sonatype.org` que tiene el POM viejo.
- `.github/workflows/release.yml` solo con `workflow_dispatch` + environment protegido **`maven-central`** con revisor. Comprueba que la versión no sea SNAPSHOT, que el tag `vX.Y.Z` apunte a HEAD y que haya sección en CHANGELOG; vuelve a pasar `verify` y los ejemplos; firma y sube **para validación**.
- **HUMAN SETUP REQUIRED** en este repo: crear el environment `maven-central` con revisor y los secrets `CENTRAL_TOKEN_USERNAME`, `CENTRAL_TOKEN_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`; proteger `main`; activar GitHub Pages si se publica el Javadoc; exportar el Swagger oficial y probar en sandbox; aprobar explícitamente cada release.
- Procedimiento detallado en `docs/release.md` (adaptar el de qvapay-java). Después de publicar, comprobar la resolución desde consumidores Maven y Gradle limpios y solo entonces actualizar el README y los badges.

## 10. Cómo trabajar

1. Seguir `PLAN.md` hito a hito, en ramas cortas (`feat/m3-core`…) con merge `--ff-only` a `main`; actualizar la tabla de estado de `PLAN.md` al cerrar cada hito.
2. **Conventional Commits** en inglés, atómicos y en verde (`./mvnw -B -ntp clean verify` pasa en cada commit), código y tests juntos, CHANGELOG `[Unreleased]` en el mismo commit que la funcionalidad. Los commits de Claude terminan con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
3. Antes de cada endpoint: fixture JSON → tests de request/response/errores → implementación → Javadoc → fila en `api-coverage.md`.
4. Reutilizar y adaptar el núcleo de `qvapay-java` (transporte, ejecutor, códec, errores, scripts, workflows) en lugar de reinventarlo, cambiando la autenticación por OAuth2.
5. Prohibido sin aprobación explícita del mantenedor: `git push`, crear tags o releases, subir a Central, tocar secrets o settings de GitHub, llamar a la API real.
6. Al terminar: resumen de archivos, operaciones cubiertas, comandos ejecutados con su resultado y lista de pasos HUMAN SETUP pendientes.

### Checklist rápido

- [ ] Rutas con `/payment/v1.0.0`, sandbox seleccionable.
- [ ] Token en caché, repetición única tras 401, secretos redactados.
- [ ] JSON snake_case e importes como string con 2 decimales en `BigDecimal`.
- [ ] `links` por `rel`; no tratar la redirección como prueba de pago.
- [ ] Sin reintentos en operaciones de dinero.
- [ ] Incertidumbres marcadas con `TODO: VERIFY WITH ENZONA` y registradas.
- [ ] Tests sin red; ejemplos que compilan; README EN/ES sin afirmaciones falsas.
