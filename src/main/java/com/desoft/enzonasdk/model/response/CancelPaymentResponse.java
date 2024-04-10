package com.desoft.enzonasdk.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelPaymentResponse {
    private String statusCode;
    private String transactionDenom;
    private String updateAt;
    private String statusDenom;
    private String transactionUuid;
}
