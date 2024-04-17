package io.github.alejo2075.enzonasdk.model.request;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    private String fundingSourceUuid;
    private String paymentPassword;
    private String fingerprint;
}
