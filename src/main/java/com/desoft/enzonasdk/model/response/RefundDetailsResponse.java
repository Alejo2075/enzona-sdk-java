package com.desoft.enzonasdk.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RefundDetailsResponse {
    private String transactionStatusCode;
    private String parentPaymentUuid;
    private String updatedAt;
    private String transactionDenom;
    private String createdAt;
    private String description;
    private List<Link> links;
    private String state;
    private String uuid;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Link {
        private String method;
        private String rel;
        private String href;
    }
}
