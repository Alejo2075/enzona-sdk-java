package io.github.alejo2075.enzona.model;

/**
 * An action link of a payment. Find the one you need by {@link #rel()}, never by position.
 *
 * <p>{@code confirm} (method {@code REDIRECT}) is where the buyer's browser goes. The other links
 * are informative: call {@code complete}, {@code cancel} or {@code refund} through the SDK, which
 * adds the access token, instead of following their {@code href}.
 *
 * @param rel the relation: {@code confirm}, {@code complete}, {@code cancel}, {@code refund} or
 *     {@code self}
 * @param method {@code REDIRECT}, {@code POST} or {@code GET}
 * @param href the target URL
 */
public record PaymentLink(String rel, String method, String href) {}
