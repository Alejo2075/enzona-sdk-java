package com.desoft.enzonasdk.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentsListRequest {
    private String merchantUuid;
    private String limit;
    private String offset;
    private String merchantOpFilter;
    private String enzonaOpFilter;
    private String statusFilter;
    private String startDateFilter;
    private String endDateFilter;
    private String orderFilter;
}
