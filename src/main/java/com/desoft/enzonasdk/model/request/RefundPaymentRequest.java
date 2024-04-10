package com.desoft.enzonasdk.model.request;

import lombok.Data;

@Data
public class RefundPaymentRequest {
    private Amount amount;
    private String commerceRefundId;
    private String username;
    private String description;

    @Data
    public static class Amount {
        private String total;
    }
}
