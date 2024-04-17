package io.github.alejo2075.enzonasdk.model.request;

import lombok.Data;

@Data
public class CreatePaymentOrderRequest {
    private long merchantOpId;
    private double amount;
    private String description;
    private String currency;
}
