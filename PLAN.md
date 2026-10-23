# PLAN.md — Reescritura del ENZONA Java SDK (2.0.0)

> Plan operativo derivado de [`CLAUDE.md`](CLAUDE.md): cubre todo el camino desde borrar el SDK 1.x roto hasta un repositorio **release-ready** y la publicación de `io.github.alejo2075:enzona-sdk:2.0.0` en Maven Central, en el mismo Portal y namespace que `qvapay-java`.
> Fecha: 23-oct-2026. Documento interno (en español). Toda la documentación pública va en inglés.

## Estado de ejecución

| Hito | Estado |
|---|---|
| M0 – Entorno y decisiones | Completado (JDK 17/21 en `~/.jdks`, coordenadas y licencia decididas) |
| M1 – Limpieza y scaffold | Completado |
| M2 – Investigación y diseño | Completado |
| M3 – Núcleo (errores, auth OAuth2, HTTP, JSON, cliente) | Completado |
| M4 – Modelos y PaymentAPI | Completado |
| M5 – Devoluciones | Completado |
| M6 – Hardening (async ya incluido en cada operación) | Completado |
| M7 – Ejemplos consumidores | Completado |
| M8 – Documentación pública (falta docs/release.md, va en M10) | Completado |
| M9 – CI (sin workflow de Pages, opcional) | Completado |
| M10 – Preparación Maven Central | Completado |
| M11 – Release 2.0.0 (gate humano) | Pendiente: necesita aprobación explícita |

## 0. Resumen

| Hito | Contenido | Resultado verificable |
|---|---|---|
| M0 | Entorno y decisiones | `~/.jdks/jdk17/bin/java -version` = 17; coordenadas fijadas |
| M1 | Eliminar 1.x, scaffold Maven e higiene OSS | `./mvnw -B -ntp clean verify` en verde con un proyecto casi vacío |
| M2 | Estudio de referencias, inventario de la API, arquitectura | `docs/references.md`, `docs/api-coverage.md`, `docs/architecture.md` |
| M3 | Errores, OAuth2 con caché, transporte, códec JSON, ejecutor, cliente | Tests de token, cabeceras, errores y redacción de secretos |
| M4 | Modelos de pago y 5 operaciones de pago | Fixture y tests por operación |
| M5 | 4 operaciones de devoluciones | Fixture y tests por operación |
| M6 | Tests transversales, umbral de cobertura (sin `ReturnUrlParser`, ver nota) | Checklist §7 de `CLAUDE.md` completo |
| M7 | `examples/` Maven, Gradle (Groovy/Kotlin) y retorno | Compilan y pasan contra el JAR de `~/.m2` |
| M8 | README EN/ES, docs, migración, archivos comunitarios | `scripts/readme_snippets.py check` en verde |
| M9 | GitHub Actions, Dependabot, gitleaks, Pages | Workflows válidos |
| M10 | POM de Central, perfil `central`, release workflow, verificador | Bundle firmado verificado **sin subirlo** |
| M11 | Release 2.0.0 | Solo con aprobación: subida al Portal, publicación manual, verificación |

Orden obligatorio: **errores → auth → HTTP/JSON → cliente → modelos → endpoints → async/hardening → ejemplos → docs → CI → release**.

---

## 1. Estrategia de commits

### 1.1 Convención: Conventional Commits 1.0

```
<type>(<scope>): <resumen en inglés, imperativo, ≤72 caracteres>

<cuerpo opcional: qué y por qué>

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
```

**Tipos:** `feat`, `fix`, `test`, `docs`, `build`, `ci`, `refactor`, `chore`, `perf`, `style`. **Ruptura:** `!` + footer `BREAKING CHANGE:` (el commit que borra 1.x lo lleva).

| Scope | Abarca |
|---|---|
| `repo` | `.gitignore`, `.editorconfig`, licencias, plantillas, borrado de legado |
| `build` | `pom.xml`, wrapper, plugins |
| `error` | jerarquía de excepciones, `EnzonaErrorCode` |
| `auth` | credenciales, `OAuthTokenProvider`, scopes |
| `http` | transporte, `ApiRequest`/`ApiResponse`, reintentos |
| `json` | códec Jackson 3 y deserializadores tolerantes |
| `core` | `EnzonaClient`, `Environment`, ejecutor |
| `payments`, `refunds` | APIs y modelos de cada grupo |
| `flow` | (sin uso: descartado el parser de la URL de retorno) |
| `examples`, `docs`, `readme`, `ci`, `release` | lo que indica su nombre |

### 1.2 Reglas

1. **Atómico:** un commit, un cambio lógico (una operación = modelo + fixture + tests + CHANGELOG + fila en `api-coverage.md`).
2. **Verde siempre:** `./mvnw -B -ntp clean verify` pasa en cada commit que llega a `main`, para que `git bisect` sirva. La excepción son los commits de M1 anteriores a tener POM, que no pueden compilar nada.
3. **Sin secretos:** `.env` ignorado desde el primer commit; gitleaks en CI.
4. Mensajes en inglés. Los TODO de la API (`TODO: VERIFY WITH ENZONA`) se registran en `api-coverage.md` en el mismo commit.

### 1.3 Ramas y merges

- Se conserva el historial de 1.x (no se reescribe la historia publicada). Antes de empezar se crea el tag **`legacy-1.0`** en `f35c566` para conservar una referencia al código publicado.
- Una rama por hito: `chore/m1-scaffold`, `docs/m2-research`, `feat/m3-core`, `feat/m4-payments`, `feat/m5-refunds`, `feat/m6-hardening`, `feat/m7-examples`, `docs/m8-docs`, `ci/m9-workflows`, `release/m10-central-ready`, `release/2.0.0`.
- Merge a `main` con `git merge --ff-only` (historial lineal con commits atómicos). Squash solo si quedó algún "wip".
- **Push:** solo cuando el mantenedor lo apruebe. Recomendación: al terminar M1 (para que el repo deje de anunciar 1.x) y después de cada hito. Cuando exista CI (M9), cada hito se integra mediante PR con los checks en verde.

### 1.4 Versionado y tags

- `main` en `2.0.0-SNAPSHOT` durante el desarrollo.
- Release: `chore(release): prepare 2.0.0` → tag anotado `v2.0.0` → publicación → `docs(readme): announce 2.0.0 on Maven Central` → `chore(release): start 2.1.0-SNAPSHOT`.
- Los tags nunca disparan la publicación. CHANGELOG en formato *Keep a Changelog* desde M3.

---

## 2. Hitos con su secuencia de commits

### M0 — Entorno y decisiones (sin commits) ✅

- JDK: `export JAVA_HOME=~/.jdks/jdk17` (Temurin 17.0.21); `~/.jdks/jdk21` para compatibilidad. No hay `java` ni `mvn` en el PATH: siempre `./mvnw`.
- Coordenadas `io.github.alejo2075:enzona-sdk:2.0.0`, paquete `io.github.alejo2075.enzona`, Apache-2.0, repo `Alejo2075/enzona-sdk-java`.
- GPG: clave `BC2FB2385127DC8345884595564BA523FA3BCCF6` en el llavero local (la misma de qvapay-java).

### M1 — Limpieza y scaffold (`chore/m1-scaffold`)

| # | Commit | Contenido |
|---|---|---|
| 1 | `docs: add agent instructions and development plan` | `CLAUDE.md`, `PLAN.md` |
| 2 | `chore(repo)!: remove the 1.x implementation` | borra `src/`, `docs/` (Javadoc generado), `.idea/` del índice, `pom.xml`, `README.md`; footer `BREAKING CHANGE` explicando que 2.0 es una reescritura |
| 3 | `chore(repo): add editorconfig, gitattributes, gitignore and Apache license` | `.editorconfig`, `.gitattributes`, `.gitignore` (target/, .idea/, .env, *.gpg…), `LICENSE.txt` → `LICENSE` |
| 4 | `build: add Maven wrapper 3.10.0` | `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` |
| 5 | `build: add base pom with Java 17 release and pinned plugins` | coordenadas 2.0.0-SNAPSHOT, BOMs Jackson 3 y JUnit 6, compiler con `-Xlint:all -Werror`, `Automatic-Module-Name`, recurso filtrado con la versión |
| 6 | `build: enforce Maven 3.9+, Java 17+ and dependency convergence` | enforcer |
| 7 | `build: add spotless formatting and jacoco coverage` | Spotless AOSP 1.27.0; JaCoCo con umbral inicial bajo |
| 8 | `build: attach sources and javadoc jars to the default build` | source + javadoc (`doclint=all`, `failOnWarnings`), `package-info.java` raíz y `EnzonaVersion` |
| 9 | `chore(repo): add .env.example with placeholders` | `ENZONA_CONSUMER_KEY=`, `ENZONA_CONSUMER_SECRET=`, `ENZONA_ENVIRONMENT=sandbox` |

**DoD:** `verify` en verde con JDK 17 y 21; JAR con bytecode major 61.

### M2 — Investigación y diseño (`docs/m2-research`)

| # | Commit | Contenido |
|---|---|---|
| 10 | `docs: add reference repository study` | `docs/references.md`: las 6 fuentes de `CLAUDE.md §2`, con tabla Inspiración / Qué no copiar / Confirmado, licencias y fechas |
| 11 | `docs: add API coverage inventory with verification status` | `docs/api-coverage.md`: 14 rutas de pago + 2 de QR + Claim/XMET con estado (`verified`, `implemented`, `tested`, `blocked`, `undocumented`), fuente, fecha, discrepancias y lista de `TODO: VERIFY` |
| 12 | `docs: add architecture design` | `docs/architecture.md`: capas, OAuth con caché y repetición tras 401, códec tolerante, dinero, reintentos, async, extensibilidad hacia QR |

### M3 — Núcleo (`feat/m3-core`)

| # | Commit | Contenido |
|---|---|---|
| 13 | `feat(error): add exception hierarchy and error code catalog` | `EnzonaException` → `EnzonaApiException` (+400/401/403/404/429/5xx), `TransportException`, `SerializationException`, `TokenException`; `EnzonaErrorCode` (catálogo 4xxx/5xxx/9009xx) |
| 14 | `feat(http): add transport abstraction, request/response models and url encoding` | `HttpTransport` (sync/async), `ApiRequest`, `ApiResponse`, `UrlEncoding` |
| 15 | `feat(http): add JDK HttpClient transport` | `JdkHttpTransport`, `Redirect.NEVER`, timeouts, conserva la interrupción; arnés de test `TestServer` + `FakeTransport` |
| 16 | `feat(json): add encapsulated Jackson 3 codec with lenient money and number handling` | `JsonCodec`, `Money` (string de 2 decimales ↔ `BigDecimal`), enteros como string, string-o-array, campos desconocidos |
| 17 | `feat(auth): add client credentials and scopes` | `ClientCredentials` (secreto redactado), `Scope` (`PAYMENT`, `QR`, personalizado), `AccessTokenProvider` |
| 18 | `feat(auth): add OAuth2 client-credentials token provider with caching` | `OAuthTokenProvider`: Basic, form, caché con `expires_in` − margen, `Clock`, concurrencia de petición única, `invalidate()` |
| 19 | `feat(http): map gateway and api errors to typed exceptions` | `ErrorMapper`: `fault` WSO2, `{code,message}`, cuerpo no JSON, `Retry-After` |
| 20 | `feat(http): add request executor with token retry on 401 and opt-in read retries` | `RequestExecutor`, `RetryPolicy` (solo GET), repetición única tras 401 |
| 21 | `feat(core): add EnzonaClient builder with environments` | `Environment.PRODUCTION/SANDBOX`, `baseUrl` HTTPS, `User-Agent`; sin `AutoCloseable` (JDK 17 no puede cerrar `HttpClient`) |

### M4 — PaymentAPI (`feat/m4-payments`)

| # | Commit | Contenido |
|---|---|---|
| 22 | `feat(payments): add payment models with tolerant deserialization` | `Payment`, `PaymentAmount`, `AmountDetails`, `PaymentItem`, `PaymentLink`, `PaymentStatus`; fixtures `payment.json` (variantes con tipos raros) |
| 23 | `feat(payments): add create payment with amount validation` | `CreatePaymentRequest` (builder, cálculo y validación del total, 2 decimales, URLs, moneda) → `POST /payments` |
| 24 | `feat(payments): add get payment details` | `GET /payments/{uuid}` |
| 25 | `feat(payments): add paginated payment list with filters` | `PaymentQuery`, `SortOrder`, `PageInfo`, `PaymentPage` → `GET /payments` |
| 26 | `feat(payments): add complete payment without retries` | `POST /payments/{uuid}/complete`; test: 5xx/429/timeout → una sola petición |
| 27 | `feat(payments): add cancel payment without retries` | `POST /payments/{uuid}/cancel` |

### M5 — Devoluciones (`feat/m5-refunds`)

| # | Commit | Contenido |
|---|---|---|
| 28 | `feat(refunds): add refund models` | `Refund` (`items[].name` como array, `parent_payment_uuid`, `refund_name`…), `RefundRequest` (total o parcial) |
| 29 | `feat(refunds): add full and partial refund without retries` | `POST /payments/{uuid}/refund` |
| 30 | `feat(refunds): add get refund details` | `GET /payments/refund/{uuid}` |
| 31 | `feat(refunds): add refund list with filters` | `RefundQuery`, `RefundPage` → `GET /payments/refunds` |
| 32 | `feat(refunds): add refunds of a payment` | `GET /payments/{uuid}/refunds` |

### M6 — Async, flujo y hardening (`feat/m6-hardening`)

| # | Commit | Contenido |
|---|---|---|
| 33 | ~~`feat(flow): add return url parser`~~ | **Descartado**: ENZONA no documenta que añada el uuid a `return_url` (el SDK Dart y el plugin de WordPress lo guardan al crear). Se documenta el patrón en `docs/payment-flow.md` |
| 34 | `test(core): verify sync and async parity over real HTTP` | |
| 35 | `test(core): ensure secrets never appear in toString, exceptions or output` | |
| 36 | `test(core): verify isolation and token sharing across threads and clients` | |
| 37 | `test(http): cover timeouts, interruption, Retry-After and path injection` | |
| 38 | `test(json): verify coexistence with Jackson 2 on the classpath` | Jackson 2 solo en scope `test` |
| 39 | `build: enforce jacoco coverage thresholds (90% lines, 80% branches)` | |

### M7 — Ejemplos (`feat/m7-examples`)

| # | Commit | Contenido |
|---|---|---|
| 40 | `feat(examples): add plain Java Maven consumer tested against a mock server` | `QuickStart`, `PaymentFlow`, `Refunds`, `AdvancedUsage` (async, errores, reintentos); marcadores `// snippet:start` |
| 41 | `feat(examples): add Gradle consumers with Kotlin and Groovy DSL` | wrapper 9.8.2, toolchain 17, `mavenLocal()` hasta la publicación |
| 42 | `feat(examples): add return url handler example` | servidor `com.sun.net.httpserver`: return → get → verificar → complete, idempotente |

### M8 — Documentación (`docs/m8-docs`)

| # | Commit | Contenido |
|---|---|---|
| 43 | `docs: add payment flow, security and troubleshooting guides` | `docs/payment-flow.md` (Mermaid), `security.md`, `troubleshooting.md` (timeouts fuera de Cuba, 900901, 4083/4084…) |
| 44 | `docs: add migration guide from 1.x` | `docs/migration-1.x.md`: tabla de equivalencias entre métodos antiguos y nuevos, y por qué 1.x no funcionaba |
| 45 | `docs(readme): add English README with compiled, synchronized snippets` | estructura de `CLAUDE.md §8`; `scripts/readme_snippets.py` |
| 46 | `docs(readme): add Spanish README` | `README.es.md` |
| 47 | `docs: add contributing guide, code of conduct and security policy` | |
| 48 | `chore(repo): add issue and pull request templates` | |
| 49 | `docs: add changelog for 2.0.0` | consolidar `[Unreleased]` |

### M9 — CI (`ci/m9-workflows`)

| # | Commit | Contenido |
|---|---|---|
| 50 | `ci: add build workflow with Java 17 and 21 matrix` | `ci.yml` (+ JDK 25 opcional sin Spotless) |
| 51 | `ci: build example consumers against the locally installed SDK` | |
| 52 | `ci: verify README snippets match the compiled examples` | |
| 53 | `ci: add secret scanning and dependency review` | gitleaks, dependency-review |
| 54 | `ci: add Dependabot for Maven, Gradle and GitHub Actions` | |
| 55 | `ci: publish Javadoc to GitHub Pages` | `pages.yml` + `scripts/` del generador (opcional; requiere activar Pages) |

### M10 — Preparación Maven Central (`release/m10-central-ready`)

| # | Commit | Contenido |
|---|---|---|
| 56 | `build(release): complete POM metadata for Maven Central` | name, description, url, licencia Apache-2.0, developer, scm (`main`), issueManagement |
| 57 | `build(release): add central profile with GPG signing and Central publishing plugin` | gpg 3.2.8 con loopback, central 0.12.0 `autoPublish=false` |
| 58 | `build(release): add local bundle verifier (no upload)` | `scripts/verify_bundle.py` (5 archivos por artefacto, `.asc`, checksums, metadatos, major 61) |
| 59 | `ci(release): add manual release workflow behind a protected environment` | `release.yml` |
| 60 | `docs(release): document the release procedure and human setup checklist` | `docs/release.md` |

**DoD:** `./mvnw -Pcentral -DskipUpload=true -DskipTests clean deploy` con versión no SNAPSHOT en una copia temporal + `verify_bundle.py` en verde (nada se sube).

### M11 — Release 2.0.0 (`release/2.0.0`, gate humano)

Requisitos previos (**HUMAN SETUP REQUIRED**):

- [ ] Crear el environment `maven-central` en `Alejo2075/enzona-sdk-java` con revisor obligatorio, limitado a ramas protegidas.
- [ ] Secrets del environment: `CENTRAL_TOKEN_USERNAME`, `CENTRAL_TOKEN_PASSWORD` (token del Portal, el mismo o uno nuevo), `GPG_PRIVATE_KEY` (export ASCII-armored de la clave de qvapay-java), `GPG_PASSPHRASE`.
- [ ] Proteger `main` (PR, checks obligatorios, sin force-push, historial lineal); activar secret scanning y Dependabot alerts.
- [ ] (Muy recomendable) Exportar el Swagger de PaymentAPI/QR desde el API Store a `docs/spec/` y hacer una prueba manual en sandbox desde Cuba.
- [ ] Aprobación explícita para publicar 2.0.0.

| # | Commit / acción | Contenido |
|---|---|---|
| 61 | `chore(release): prepare 2.0.0` | versión 2.0.0, fecha en CHANGELOG, versión en `examples/` |
| — | tag `v2.0.0` + push (con aprobación) | |
| — | Actions → *Release (manual)* → aprobar el environment | sube **para validación** |
| — | Publicar a mano en `central.sonatype.com/publishing/deployments` | esperar *Published* |
| — | Verificar desde consumidores Maven y Gradle limpios (`mavenCentral()`) | |
| 62 | `docs(readme): announce 2.0.0 on Maven Central` | coordenadas reales, badge, `examples/` contra Central, `relocation`/nota en la release de GitHub |
| 63 | `chore(release): start 2.1.0-SNAPSHOT` | |

---

## 3. Decisiones de arquitectura

| Tema | Decisión |
|---|---|
| Base | Portar el núcleo de `qvapay-java` (mismo autor) y cambiar la autenticación de app por OAuth2 con caché |
| DTOs | `record` para respuestas; builders inmutables para requests |
| Dinero | `BigDecimal` serializado como `"0.00"`; más de 2 decimales → `IllegalArgumentException` en `build()` |
| Tipos inconsistentes | Deserializadores dedicados (`LenientInteger`, `StringOrArray`, `LenientMoney`), probados con fixtures |
| Estados | `PaymentStatus` con `UNKNOWN` y `rawCode()` |
| Token | Caché por cliente; `AccessTokenProvider` reemplazable; repetición única tras 401 |
| Reintentos | Solo `GET` y opt-in; nunca `POST` |
| Async | Método `...Async` por operación (`CompletableFuture`), igual que qvapay-java |
| Retorno | Sin webhooks ni parser de URL: el integrador guarda el uuid al crear; documentación del patrón get → verificar → complete |
| Errores | Una sola jerarquía unchecked; `errorCode` numérico de ENZONA o del gateway |
| Dependencias de runtime | Solo Jackson 3 (databind + annotations) |

## 4. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| No se puede leer el Swagger oficial (host inaccesible fuera de Cuba) | Contrato reconstruido con 4 fuentes independientes; `TODO: VERIFY` explícitos; códec tolerante; prueba manual en sandbox antes de la release |
| La API devuelve tipos distintos a los documentados | Deserializadores tolerantes y fixtures con todas las variantes conocidas |
| El formato de los filtros de fecha y de la paginación es incierto | Formateo centralizado y `PageInfo` tolerante; corregir en un patch sin romper la API |
| Usuarios de 1.0 que actualicen sin leer | Major 2.0.0, guía de migración, nota en el CHANGELOG y en la release de GitHub |
| Publicar por error | `autoPublish=false`, workflow manual con environment protegido, aprobación humana |
| Fuga de secretos | Redacción en `toString`/excepciones, gitleaks, `.env` ignorado, tests específicos |

## 5. Después de 2.0.0

- **2.1.0 — QR API**: `POST /qr/merchant`, `GET /qr/{qr_code}`, scope `enzona_business_qr`; la verificación de `notify_url` solo cuando haya un contrato de firma.
- **2.2.0** — `payment-orders` y `checkout` cuando se conozca su semántica.
- Claim API y XMET cuando haya contrato.
- Revisión de compatibilidad binaria con `japicmp` desde 2.0.0.
