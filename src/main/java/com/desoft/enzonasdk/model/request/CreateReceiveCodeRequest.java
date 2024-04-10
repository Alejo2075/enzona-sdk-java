package com.desoft.enzonasdk.model.request;

import lombok.Data;

@Data
public class CreateReceiveCodeRequest {
    private String amount;
    private String fundingSourceUuid;
    private String paymentPassword;
    private String fingerprint;
    private String description;
    private String vendorIdentityCode;
    private String currency;
    private String cashAdvance;
    private String phone;
}
