package com.desoft.enzonasdk.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefundsListRequest {
    private String merchantUuid;
    private String transactionUuid;
    private String commerceRefundId;
    private String limit;
    private String offset;
    private String statusFilter;
    private String startDateFilter;
    private String endDateFilter;
    private String orderFilter;


}
